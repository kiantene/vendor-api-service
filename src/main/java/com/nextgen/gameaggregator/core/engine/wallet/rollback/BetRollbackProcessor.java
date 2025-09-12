package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.common.ClientApiRequest;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.common.OperatorApiCallerV2;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.GameCategory;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.service.GameCategoryDataService;
import com.nextgen.gameaggregator.core.service.VendorDataService;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.entity.ga.BetHistoryV3;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetRollbackProcessor {
    private final GameRoundService gameRoundService;
    private final ClientRequestService clientRequestService;
    private final OperatorApiCallerV2 operatorApiCaller;
    private final AgentDataService agentDataService;
    private final GameCategoryDataService gameCategoryDataService;
    private final VendorDataService vendorDataService;

    // TODO: temporary, will move to BetHistoryProducer
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    public PlayerBalanceData process(BetRollbackContext context, GameTransaction txn, BetRollbackConfig config) {
        String docId = txn.getRoundDocId();
        Optional<GameRound> roundOpt = gameRoundService.get(docId);

        if (roundOpt.isEmpty()) {
            // throw not found
            return defaultBalanceData(context, "");
        }

        GameRound round = roundOpt.get();

        List<RoundTxn> txnList = round.getTransactions();

        if (round.isSettled()) {
            if (config.isAllowRollbackForSettledBet()) {
                produceBetHistory(context, round);
            } else {
                throw new RollbackNotAllowedException("Round already settled.");
            }
        }

        txnList.stream()
                .filter(RoundTxn::isSuccessfulBetOrResult)
                .forEach(t -> callToOperator(context, round, t));

        // TODO: update status to rollback
        return defaultBalanceData(context, round.getCurrency());
    }

    private PlayerBalanceData defaultBalanceData(BetRollbackContext context, String currency) {
        return PlayerBalanceData.getDefault(context.getVendorPlayerUsername(), currency);
    }

    private PlayerBalanceData callToOperator(BetRollbackContext context, GameRound round, RoundTxn txn) {
        try {
            ClientApiRequest<WalletRollbackDto> apiRequest = clientRequestService.createClientApiRequest(
                    round.getAgentMeta().getAgentId(),
                    EndPoints.WALLET_ROLLBACK,
                    mapToClientRequest(context, round, txn)
            );

            ClientBalanceResponse response = operatorApiCaller.post(
                    apiRequest.getBaseUrl(),
                    apiRequest.getPath(),
                    apiRequest.getHeaders(),
                    apiRequest.getRequestObject()
            );

            return response.getData();
        } catch (Exception ex) {

            throw ex;
        }
    }

    private WalletRollbackDto mapToClientRequest(BetRollbackContext context, GameRound round, RoundTxn txn) {
        WalletRollbackDto dto = new WalletRollbackDto();

        dto.setTraceId(context.getTraceId());
        dto.setTransactionId(UuidUtil.newUuidV7String());
        dto.setBetId(txn.getGaBetId());
        dto.setExternalTransactionId(context.getIdempotencyKey());
        dto.setRoundId(round.getRoundId());
        dto.setGameCode(round.getAgentMeta().getGameCode());
        dto.setUsername(round.getAgentMeta().getUsername());
        dto.setCurrency(round.getAgentMeta().getCurrency());
        dto.setTimestamp(context.getTimestamp());

        return dto;
    }

    private void produceBetHistory(BetRollbackContext context, GameRound round) {
        BigDecimal bet      = round.getBetAmount();
        BigDecimal win      = round.getWinAmount();
        BigDecimal winLoss  = win.subtract(bet);
        BigDecimal turnover = bet;
        BigDecimal jackpot  = BigDecimal.ZERO; // TODO: implement when we have jackpot use case

        Agent agent = agentDataService.get(round.getAgentMeta().getAgentId());
        GameCategory gameCategory = gameCategoryDataService.get(context.getGameCategoryId());
        Vendor vendor = vendorDataService.get(context.getVendorId());

        // TODO: conversion rate

        BetHistoryV3 betHistoryV3 = new BetHistoryV3();

        betHistoryV3.setId(UuidUtil.newUuidV7String());
        betHistoryV3.setExternalTransactionId(context.getIdempotencyKey());
        betHistoryV3.setVendorBetId(context.getVendorBetId());
        betHistoryV3.setRoundId(round.getRoundId());
        betHistoryV3.setProductId(vendor.getProductId());
        betHistoryV3.setProductCode("");
        betHistoryV3.setProductGameId(0);
        betHistoryV3.setVendorGameId(context.getVendorGameId());
        betHistoryV3.setVendorPlayerId(context.getVendorPlayerId());
        betHistoryV3.setVendorId(context.getVendorId());
        betHistoryV3.setVendorCode(vendor.getCode());
        betHistoryV3.setVendorLineId(context.getVendorLineId());
        betHistoryV3.setAgentPlayerId(context.getAgentPlayerId());
        betHistoryV3.setHouseId(agent.getHouseId());
        betHistoryV3.setMasterAgentId(agent.getMasterAgentId());
        betHistoryV3.setAgentId(agent.getId());
        betHistoryV3.setOperatorStatus(0);
        betHistoryV3.setGameCategoryId(gameCategory.getId());
        betHistoryV3.setCurrencyId(context.getCurrencyId());
        betHistoryV3.setCurrencyCode(round.getAgentMeta().getCurrency());
        betHistoryV3.setBetAmount(bet.negate());
        betHistoryV3.setWinAmount(win.negate());
        betHistoryV3.setWinLoss(winLoss.negate());
        betHistoryV3.setEffectiveTurnover(turnover.negate());
        betHistoryV3.setJackpotAmount(jackpot.negate());
        betHistoryV3.setResultType(BetResultType.BET.code);
        betHistoryV3.setBetType(BetType.NORMAL_BET.code);
        betHistoryV3.setIsFreespin(0);
        betHistoryV3.setResettleNum(0);
        betHistoryV3.setStatus(BetStatus.CANCELLED.code);
        betHistoryV3.setGameSessionToken(round.getAgentMeta().getSession());
        betHistoryV3.setVendorBetTime(context.getTimestamp());
        betHistoryV3.setVendorSettleTime(context.getTimestamp());
        betHistoryV3.setResultTime(System.currentTimeMillis());
        betHistoryV3.setGameCode(round.getAgentMeta().getGameCode());
        betHistoryV3.setVendorPlayerUsername(context.getVendorPlayerUsername());
        betHistoryV3.setAgentPlayerUsername(round.getAgentMeta().getUsername());
        betHistoryV3.setGameCategoryCode(gameCategory.getCode());

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(betHistoryV3);

            CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(KafkaConstant.TOPIC_BET_HISTORY_V3, json);

            future.exceptionally(throwable -> {
                log.error("Error sending BetHistoryV3 to Kafka: ", throwable);
                return null;
            });
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }
}
