package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.common.ClientApiRequest;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.retry.RetryHelper;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.service.LegacyCleanupService;
import com.nextgen.gameaggregator.core.webclient.OperatorApiCaller;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetRollbackProcessor {
    private final BetRollbackContextEnricher enricher;
    private final GameRoundService gameRoundService;
    private final GameTransactionService gameTransactionService;
    private final ClientRequestService clientRequestService;
    private final OperatorApiCaller operatorApiCaller;
    private final BetHistoryProducer betHistoryProducer;
    private final RetryQueueService retryQueueService;
    private final LegacyCleanupService legacyCleanupService;

    public PlayerBalanceData processBetRollback(BetRollbackContext context, GameTransaction txn, BetRollbackConfig config) {
        Optional<GameTransaction> betTxnOpt = gameTransactionService.get(txn.getRollbackId());

        if (betTxnOpt.isEmpty()) {
            // throw exception
            return defaultBalanceData(context, "");
        }

        GameTransaction betTxn = betTxnOpt.get();

        if (betTxn.isSettled() && (!config.isAllowRollbackForSettledBet())) {
            throw new RollbackNotAllowedException("Transaction already settled");
        }

        Optional<GameRound> roundOpt = gameRoundService.get(betTxn.getRoundDocId());

        if (roundOpt.isEmpty()) {
            log.error(betTxn.getRoundDocId() + " cannot be found");
            // should not happen but return default balance just in case it happens.
            return defaultBalanceData(context, "");
        }

        GameRound round = roundOpt.get();

        enricher.enrichByGameRound(context, round, txn);

        if (betTxn.isSettled()) {
            // TODO: should only send message for 1 txn
            betHistoryProducer.publishCancelledBetHistory(context, round);
        }

        legacyCleanupService.cleanup(round, betTxn.getVendorBetId(), context.getVendorGameId(), context.getVendorPlayerId());
        gameTransactionService.markSent(txn, null);
        // TODO: check status to decide send or don't send to operator
        PlayerBalanceData balanceData = callToOperator(context, round, betTxn.getGaBetId());
        gameTransactionService.markRefunded(betTxn.getId());
        gameTransactionService.markRollback(round, txn, balanceData.getBalance());

        return new PlayerBalanceData(
                round.getUsername(),
                round.getCurrency(),
                balanceData.getBalance(),
                balanceData.getTimestamp()
        );
    }

    public PlayerBalanceData processRoundRollback(BetRollbackContext context, GameTransaction txn, BetRollbackConfig config) {
        String docId = txn.getRoundDocId();
        Optional<GameRound> roundOpt = gameRoundService.get(docId);

        if (roundOpt.isEmpty()) {
            // throw not found
            return defaultBalanceData(context, "");
        }

        GameRound round = roundOpt.get();

        enricher.enrichByGameRound(context, round, txn);

        if (round.isSettled()) {
            if (!config.isAllowRollbackForSettledBet()) {
                throw new RollbackNotAllowedException("Round already settled");
            }
            betHistoryProducer.publishCancelledBetHistory(context, round);
        }

        var byGaBetId = round.getTransactions().stream()
                .filter(RoundTxn::isSuccessfulBetOrResult)
                .filter(t -> t.getGaBetId() != null)
                .collect(Collectors.groupingBy(RoundTxn::getGaBetId));

        gameTransactionService.markSent(txn, null);

        PlayerBalanceData balanceData = new PlayerBalanceData();
        for (Map.Entry<String, List<RoundTxn>> entry : byGaBetId.entrySet()) {
            String gaBetId = entry.getKey();

            // 1. Call once per gaBetId -> overwrite each time
            balanceData = callToOperator(context, round, gaBetId);

            // 2. Update every txn in this group
            entry.getValue().forEach(t -> {
                gameTransactionService.markRefunded(t.getId());
                legacyCleanupService.cleanup(round, t.getVendorBetId(), context.getVendorGameId(), context.getVendorPlayerId());
            });
        }

        BigDecimal balance = Optional.ofNullable(balanceData.getBalance()).orElse(round.getLastBalance());
        gameTransactionService.markRollback(round, txn, balance);

        return new PlayerBalanceData(
                round.getUsername(),
                round.getCurrency(),
                balance,
                context.getTimestamp()
        );
    }

    private PlayerBalanceData defaultBalanceData(BetRollbackContext context, String currency) {
        return PlayerBalanceData.getDefault(context.getVendorPlayerUsername(), currency);
    }

    private PlayerBalanceData callToOperator(BetRollbackContext context, GameRound round, String gaBetId) {
        ClientApiRequest<WalletRollbackDto> apiRequest = null;
        try {
            apiRequest = clientRequestService.createClientApiRequest(
                    round.getAgentMeta().getAgentId(),
                    EndPoints.WALLET_ROLLBACK,
                    mapToClientRequest(context, round, gaBetId)
            );

            ClientBalanceResponse response = operatorApiCaller.post(
                    apiRequest.getBaseUrl(),
                    apiRequest.getPath(),
                    apiRequest.getHeaders(),
                    apiRequest.getRequestObject()
            );

            return response.getData();
        } catch (Exception ex) {
            if (apiRequest != null) {
                retryQueueService
                        .enqueue(RetryHelper.toHttpCallSpec(apiRequest))
                        .subscribe();
            }

            throw ex;
        }
    }

    private WalletRollbackDto mapToClientRequest(BetRollbackContext context, GameRound round, String betId) {
        WalletRollbackDto dto = new WalletRollbackDto();

        dto.setTraceId(context.getTraceId());
        dto.setTransactionId(UuidUtil.newUuidV7String());
        dto.setBetId(betId);
        dto.setExternalTransactionId(context.getIdempotencyKey());
        dto.setRoundId(round.getRoundId());
        dto.setGameCode(round.getAgentMeta().getGameCode());
        dto.setUsername(round.getAgentMeta().getUsername());
        dto.setCurrency(round.getAgentMeta().getCurrency());
        dto.setTimestamp(context.getTimestamp());

        return dto;
    }
}
