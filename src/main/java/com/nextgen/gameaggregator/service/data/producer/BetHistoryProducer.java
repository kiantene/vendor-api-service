package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.AgentPlayer;
import com.nextgen.gameaggregator.core.entity.GameCategory;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.entity.VendorCurrency;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.service.CurrencyConversionService;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.service.WarehouseBetHistoryService;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import com.nextgen.gameaggregator.service.data.model.TxnAmounts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetHistoryProducer {
    private final CurrencyConversionService currencyConversionService;
    private final KafkaService kafkaService;
    private final AgentDataService agentDataService;
    private final AgentPlayerDataService agentPlayerDataService;
    private final GameCategoryDataService gameCategoryDataService;
    private final VendorCurrencyDataService vendorCurrencyService;
    private final VendorPlayerService vendorPlayerService;
    private final VendorDataService vendorDataService;
    private final BetHistoryMapper betHistoryMapper;
    private final BetTxnToBetHistoryMapper betTxnToBetHistoryMapper;
    private final WarehouseBetHistoryService warehouseBetHistoryService;
    private record PlayerUsernames(String agentPlayer, String vendorPlayer) {}

    public void publish(BetHistoryV3 betHistory) {
        kafkaService.produceBetHistoryV3(betHistory);
    }

    public void publish(SettledBet settledBet, BetHistoryPublishContext context) {
        publish(settledBet, context, BetResultConfig.ProcessingMode.SINGLE);
    }

    public void publish(SettledBet settledBet,
                        BetHistoryPublishContext context,
                        BetResultConfig.ProcessingMode processingMode) {

        BetHistory betHistory = buildBetHistory(settledBet, context);
        PlayerUsernames usernames = getUsernamesIfNull(
                context,
                betHistory.getAgentPlayerId(),
                betHistory.getVendorPlayerId()
        );

        if (context.requirePreprocessing()) {
            kafkaService.producePreprocessingBetHistory(
                    betHistory,
                    usernames.agentPlayer(),
                    usernames.vendorPlayer(),
                    context.fromVendorRate()
            );
            return;
        }

        if (processingMode.isSingleMode()) {
            BetHistoryV3 betHistoryV3 = produceBetHistory(
                    betHistory,
                    context,
                    usernames
            );

            produceBetHistoryUncap(betHistoryV3, settledBet, context);
        } else {
            produceBetHistoryBatch(context.txnList(), settledBet, context, usernames);
            // TODO: to finalise solution for uncap bet history
        }
    }

    public void publishCancelledBetHistory(BetRollbackContext context, GameRound round) {
        Agent agent = agentDataService.get(round.getAgentMeta().getAgentId());
        GameCategory gameCategory = gameCategoryDataService.get(context.getGameCategoryId());
        Vendor vendor = vendorDataService.get(context.getVendorId());

        BetHistoryV3 betHistory = buildCancelledBetHistory(context, round, agent, vendor, gameCategory);
        publish(betHistory);
    }

    public void publishBetHistoryByRound(BetResultContext betResultContext, GameRound round, GameTransaction txn) {
        BetHistoryContext context = BetHistoryContext.of(betResultContext);
        BetHistoryV3 betHistory = betHistoryMapper.initialise(context, txn.getGaBetId(), txn.getTransactionId());

        Integer vendorId = context.getVendorId();
        Agent agent = agentDataService.get(round.getAgentMeta().getAgentId());
        GameCategory gameCategory = gameCategoryDataService.get(context.getGameCategoryId());
        Vendor vendor = vendorDataService.get(vendorId);
        VendorCurrency vendorCurrency = vendorCurrencyService.getVendorIdAndCurrencyId(vendorId, context.getCurrencyId());

        betHistoryMapper.mapReferenceFields(betHistory, agent, vendor, gameCategory);

        TxnAmounts txnAmounts = TxnAmounts.of(round, vendorCurrency.getFromVendorRate());

        betHistoryMapper.mapTransactionFields(
                betHistory,
                round,
                txnAmounts,
                txn.getVendorBetId(),
                txn.getBetTime(),
                txn.getSettleTime()
        );

        publish(betHistory);
    }

    public void publishBetHistoryForRollback(BetRollbackContext rollbackContext, GameRound round, GameTransaction txn) {
        BetHistoryContext context = BetHistoryContext.of(rollbackContext);
        BetHistoryV3 betHistory = betHistoryMapper.initialise(context, txn.getGaBetId(), txn.getTransactionId());

        Agent agent = agentDataService.get(round.getAgentMeta().getAgentId());
        GameCategory gameCategory = gameCategoryDataService.get(context.getGameCategoryId());
        Vendor vendor = vendorDataService.get(context.getVendorId());

        betHistoryMapper.mapReferenceFields(betHistory, agent, vendor, gameCategory);

        TxnAmounts txnAmounts = TxnAmounts.of(txn, rollbackContext.getFromVendorRate());

        betHistoryMapper.mapTransactionFields(
                betHistory,
                round,
                txnAmounts,
                txn.getVendorBetId(),
                txn.getBetTime(),
                txn.getSettleTime()
        );

        if (txn.isSettled()) {
            betHistoryMapper.negateAmounts(betHistory);
            betHistory.setStatus(BetStatus.CANCELLED.code);
            betHistory.setResettleNum(1);
        } else {
            betHistory.setStatus(BetStatus.REFUNDED.code);
        }

        publish(betHistory);
    }

    private PlayerUsernames getUsernamesIfNull(BetHistoryPublishContext context,
                                               Long agentPlayerId,
                                               Long vendorPlayerId) {

        String agentPlayerUsername = context.agentPlayerUsername();
        if (agentPlayerUsername == null) {
            AgentPlayer agentPlayer = agentPlayerDataService.get(agentPlayerId);
            agentPlayerUsername = agentPlayer.getUsername();
        }

        String vendorPlayerUsername = context.vendorPlayerUsername();
        if (vendorPlayerUsername == null) {
            try {
                VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(vendorPlayerId, null);
                vendorPlayerUsername = vendorPlayer.getUsername();
            } catch (InvalidPlayerException ex) {
                vendorPlayerUsername = "";
            }
        }

        return new PlayerUsernames(agentPlayerUsername, vendorPlayerUsername);
    }

    private WarehouseFutureEntity getFutureEntityForBetHistory(BetHistory betHistory) {
        return warehouseBetHistoryService.getWarehouseBetHistoryInfoCache(
                betHistory.getVendorGameId(),
                betHistory.getVendorId(),
                betHistory.getGameCategoryId(),
                betHistory.getCurrencyId(),
                betHistory.getAgentId()
        );
    }

    private BetHistory buildBetHistory(SettledBet settledBet, BetHistoryPublishContext context) {

        BetHistory betHistory = new BetHistory(settledBet);
        currencyConversionService.doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(
                betHistory,
                context.fromVendorRate()
        );

        if (betHistory.getGameSessionToken() == null) {
            betHistory.setGameSessionToken("");
        }

        return betHistory;
    }

    public BetHistoryV3 buildBetHistoryV3(BetHistory betHistory,
                                          BetHistoryPublishContext context,
                                          PlayerUsernames usernames) {

        return new BetHistoryV3(
                betHistory,
                context.productCode(),
                context.productId(),
                context.productGameId(),
                usernames.agentPlayer(),
                usernames.vendorPlayer(),
                this.getFutureEntityForBetHistory(betHistory)
        );
    }

    private BetHistoryV3 produceBetHistory(BetHistory betHistory,
                                           BetHistoryPublishContext context,
                                           PlayerUsernames usernames) {

        BetHistoryV3 betHistoryV3 = buildBetHistoryV3(betHistory, context, usernames);

        kafkaService.produceBetHistoryV3(betHistoryV3);

        return betHistoryV3;
    }

    private void produceBetHistoryUncap(BetHistoryV3 betHistoryV3, SettledBet settledBet, BetHistoryPublishContext context) {
        if (settledBet.getUncapWinAmount() == null) return;

        BigDecimal fromVendorRate = context.fromVendorRate();

        BetHistoryUncap betHistoryUncap = BetHistoryUncap.copyOf(betHistoryV3);
        betHistoryUncap.setUncapWinAmount(TxnAmount.of(settledBet.getUncapWinAmount(), fromVendorRate).amount());
        betHistoryUncap.setUncapJackpotAmount(TxnAmount.of(settledBet.getUncapJackpotAmount(), fromVendorRate).amount());
        betHistoryUncap.setUncapWinLoss(TxnAmount.of(settledBet.getUncapWinLoss(), fromVendorRate).amount());
        betHistoryUncap.setUncapEffectiveTurnover(TxnAmount.of(settledBet.getUncapEffectiveTurnover(), fromVendorRate).amount());

        kafkaService.produceBetHistoryUncap(betHistoryUncap);
    }

    private void produceBetHistoryBatch(List<BetTransaction> betTransactions,
                                        SettledBet settledBet,
                                        BetHistoryPublishContext context,
                                        PlayerUsernames usernames) {

        if (betTransactions == null || betTransactions.isEmpty()) return;

        betTransactions
                .forEach(betTxn -> {
                    BetHistory betHistory = betTxnToBetHistoryMapper.mapValues(
                            buildBetHistory(settledBet, context),
                            betTxn
                    );
                    betHistory.setVendorBetTime(settledBet.getVendorBetTime());
                    produceBetHistory(betHistory, context, usernames);
                });
    }

    // TODO: refactor
    private BetHistoryV3 buildCancelledBetHistory(BetRollbackContext context,
                                                  GameRound round,
                                                  Agent agent,
                                                  Vendor vendor,
                                                  GameCategory gameCategory) {

        BigDecimal bet      = round.getBetAmount();
        BigDecimal win      = round.getWinAmount();
        BigDecimal winLoss  = win.subtract(bet);
        BigDecimal turnover = bet;
        BigDecimal jackpot  = round.getJackpotAmount();

        // TODO: conversion rate

        BetHistoryV3 betHistory = new BetHistoryV3();

        betHistory.setId(UuidUtil.newUuidV7String());
        betHistory.setExternalTransactionId(context.getIdempotencyKey());
        betHistory.setVendorBetId(context.getVendorBetId());
        betHistory.setRoundId(round.getRoundId());
        betHistory.setProductId(vendor.getProductId());
        betHistory.setProductCode("");
        betHistory.setProductGameId(0);
        betHistory.setVendorGameId(context.getVendorGameId());
        betHistory.setVendorPlayerId(context.getVendorPlayerId());
        betHistory.setVendorId(round.getVendorId());
        betHistory.setVendorCode(vendor.getCode());
        betHistory.setVendorLineId(context.getVendorLineId());
        betHistory.setAgentPlayerId(context.getAgentPlayerId());
        betHistory.setHouseId(agent.getHouseId());
        betHistory.setMasterAgentId(agent.getMasterAgentId());
        betHistory.setAgentId(agent.getId());
        betHistory.setOperatorStatus(0);
        betHistory.setGameCategoryId(gameCategory.getId());
        betHistory.setCurrencyId(context.getCurrencyId());
        betHistory.setCurrencyCode(round.getAgentMeta().getCurrency());
        betHistory.setBetAmount(bet.negate());
        betHistory.setWinAmount(win.negate());
        betHistory.setWinLoss(winLoss.negate());
        betHistory.setEffectiveTurnover(turnover.negate());
        betHistory.setJackpotAmount(jackpot.negate());
        betHistory.setResultType(BetResultType.BET.code);
        betHistory.setBetType(BetType.NORMAL_BET.code);
        betHistory.setIsFreespin(0);
        betHistory.setResettleNum(1);
        betHistory.setStatus(BetStatus.CANCELLED.code);
        betHistory.setGameSessionToken(round.getAgentMeta().getSession());
        betHistory.setVendorBetTime(context.getTimestamp());
        betHistory.setVendorSettleTime(context.getTimestamp());
        betHistory.setResultTime(System.currentTimeMillis());
        betHistory.setGameCode(round.getAgentMeta().getGameCode());
        betHistory.setVendorPlayerUsername(round.getUsername());
        betHistory.setAgentPlayerUsername(round.getAgentMeta().getUsername());
        betHistory.setGameCategoryCode(gameCategory.getCode());

        return betHistory;
    }
}
