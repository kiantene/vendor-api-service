package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.core.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.retry.RetryHelper;
import com.nextgen.gameaggregator.core.retry.RetryOrigin;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.service.LegacyCleanupService;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
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
    private final ClientResponseValidator clientResponseValidator;
    private final OperatorApiCaller operatorApiCaller;
    private final BetHistoryProducer betHistoryProducer;
    private final RetryQueueService retryQueueService;
    private final LegacyCleanupService legacyCleanupService;

    public PlayerBalanceData processBetRollback(BetRollbackContext context, GameTransaction rollbackTxn, BetRollbackConfig config) {
        GameTransaction betTxn = gameTransactionService.get(rollbackTxn.getRollbackId())
                .orElseThrow(() -> new RecordNotFoundException("GameTransaction not found for processBetRollback: " + rollbackTxn.getRollbackId()));

        RollbackDecision decision = RollbackPolicy.decide(betTxn, config);

        if (decision.isRejected()) {
            throw new RollbackNotAllowedException(decision.reason());
        }

        if (decision.isNoOp()) {
            return defaultBalanceData(context, betTxn.getCurrency());
        }

        GameRound round = gameRoundService.get(betTxn.getRoundDocId())
                .orElseThrow(() -> new InternalServerException("GameRound not found for processBetRollback: " + betTxn.getRoundDocId()));

        enricher.enrichByGameRound(context, round, rollbackTxn);

        if (betTxn.isSettled()) {
            betHistoryProducer.publishCancelledBetHistory(context, round);
        }

        // Update bet txn status to refunded
        gameTransactionService.markRefunded(betTxn.getId());
        gameTransactionService.markSent(rollbackTxn, null);
        legacyCleanupService.cleanup(round, betTxn.getVendorBetId(), context.getVendorGameId(), context.getVendorPlayerId());

        PlayerBalanceData balanceData = callToOperator(context, round, betTxn.getGaBetId());

        gameTransactionService.markRollback(round, rollbackTxn, balanceData.getBalance());

        // translate to vendor's username/currency
        return new PlayerBalanceData(
                round.getUsername(),
                round.getCurrency(),
                balanceData.getBalance(),
                balanceData.getTimestamp()
        );
    }

    public PlayerBalanceData processRoundRollback(BetRollbackContext context, GameTransaction txn, BetRollbackConfig config) {
        GameRound round = gameRoundService.get(txn.getRoundDocId())
                .orElseThrow(() -> new RecordNotFoundException("GameRound not found for processRoundRollback: " + txn.getRoundDocId()));

        RollbackDecision decision = RollbackPolicy.decide(round, config);

        if (decision.isRejected()) {
            throw new RollbackNotAllowedException(decision.reason());
        }

        if (decision.isNoOp()) {
            return defaultBalanceData(context, round.getCurrency());
        }

        enricher.enrichByGameRound(context, round, txn);

        if (round.isSettled()) {
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

        // translate to vendor's username/currency
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
        // any exception thrown here is considered internal error
        WalletRollbackDto requestDto = mapToClientRequest(context, round, gaBetId);
        var apiRequest = clientRequestService.createClientApiRequest(
                requestDto.getTraceId(),
                round.getAgentMeta().getAgentId(),
                EndPoints.WALLET_ROLLBACK,
                requestDto
        );

        try {
            ClientBalanceResponse response = operatorApiCaller.post(
                    apiRequest.getBaseUrl(),
                    apiRequest.getPath(),
                    apiRequest.getHeaders(),
                    apiRequest.getRequestObject()
            );

            clientResponseValidator.validate(response, new ClientResponseValidator.RequestRecord(
                    requestDto.getTraceId(),
                    requestDto.getUsername(),
                    requestDto.getCurrency()
            ), context);

            return response.getData();
        } catch (Exception ex) { // only operator exception then send to retry queue
            if (shouldRetry(ex)) {
                retryQueueService
                        .enqueue(RetryHelper.toHttpCallSpec(apiRequest), RetryOrigin.BET_ROLLBACK)
                        .subscribe();
            }
        }
        // if operator exception then return default balance
        return defaultBalanceData(context, round.getCurrency());
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

    private boolean shouldRetry(Exception ex) {
        // if insufficient balance, it is assumed the operator has processed the rollback successfully.
        return !(ex instanceof InsufficientBalanceException);
    }
}
