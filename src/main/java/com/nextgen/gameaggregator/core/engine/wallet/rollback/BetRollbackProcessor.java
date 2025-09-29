package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.exception.RoundNotFoundException;
import com.nextgen.gameaggregator.core.retry.RetryHelper;
import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import com.nextgen.gameaggregator.core.retry.RetryPolicy;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.service.LegacyCleanupService;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.ClientApiResult;
import com.nextgen.gameaggregator.core.webclient.DefaultOperatorCallerLifeCycle;
import com.nextgen.gameaggregator.core.webclient.OperatorApiCaller;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
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
        // There is a possibility that the alias txn is not created yet if an error is encountered before markSent is called
        GameTransaction betTxn = gameTransactionService.get(rollbackTxn.getRollbackId())
                .orElseThrow(() -> new BetNotFoundException("GameTransaction not found for processBetRollback: " + rollbackTxn.getRollbackId()));

        RollbackDecision decision = RollbackPolicy.decide(betTxn, config);
        decision.throwIfRejected(context);

        if (decision.isNoOp()) {
            return defaultBalanceData(context, betTxn.getCurrency());
        }

        GameRound round = gameRoundService.get(betTxn.getRoundDocId())
                .orElseThrow(() -> new RoundNotFoundException("GameRound not found for processBetRollback: " + betTxn.getRoundDocId()));

        enricher.enrichByGameRound(context, round, rollbackTxn, betTxn);

        betHistoryProducer.publishBetHistoryForRollback(context, round, betTxn);

        // Update bet txn status to refunded
        gameTransactionService.markRefunded(betTxn.getId());
        gameTransactionService.markSent(rollbackTxn, null);
        legacyCleanupService.cleanup(round, betTxn.getVendorBetId(), context.getVendorGameId(), context.getVendorPlayerId());

        PlayerBalanceData balanceData = callToOperator(context, round, betTxn.getGaBetId());

        // we save the original balance from operator
        gameTransactionService.markRollback(round, rollbackTxn, balanceData.getBalance());

        TxnAmount playerBalance = TxnAmount.of(balanceData.getBalance(), context.getToVendorRate());

        // translate to vendor's username/currency
        return new PlayerBalanceData(
                round.getUsername(),
                round.getCurrency(),
                playerBalance.amount(),
                balanceData.getTimestamp()
        );
    }

    public PlayerBalanceData processRoundRollback(BetRollbackContext context, GameTransaction rollbackTxn, BetRollbackConfig config) {
        GameRound round = gameRoundService.get(rollbackTxn.getRoundDocId())
                .orElseThrow(() -> new BetNotFoundException("GameRound not found for processRoundRollback: " + rollbackTxn.getRoundDocId()));

        RollbackDecision decision = RollbackPolicy.decide(round, config);
        decision.throwIfRejected(context);

        if (decision.isNoOp()) {
            return defaultBalanceData(context, round.getCurrency());
        }

        enricher.enrichByGameRound(context, round, rollbackTxn);

        if (round.isSettled()) {
            betHistoryProducer.publishCancelledBetHistory(context, round);
        }

        var byGaBetId = round.getTransactions().stream()
                .filter(RoundTxn::isSuccessfulBetOrResult)
                .filter(t -> t.getGaBetId() != null)
                .collect(Collectors.groupingBy(RoundTxn::getGaBetId));

        gameTransactionService.markSent(rollbackTxn, null);

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
        gameTransactionService.markRollback(round, rollbackTxn, balance);

        TxnAmount playerBalance = TxnAmount.of(balance, context.getToVendorRate());

        // translate to vendor's username/currency
        return new PlayerBalanceData(
                round.getUsername(),
                round.getCurrency(),
                playerBalance.amount(),
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
                requestDto.getUsername(),
                EndPoints.WALLET_ROLLBACK,
                requestDto,
                requestDto.getTimestamp()
        );

        try {
            ClientApiResult apiResult = operatorApiCaller.post(apiRequest, DefaultOperatorCallerLifeCycle.get());
            apiResult.throwIfError();
            ClientApiResponse response = apiResult.parseTo(ClientApiResponse.class);

            clientResponseValidator.validate(response, new ClientResponseValidator.RequestRecord(
                    "wallet.rollback",
                    requestDto.getTraceId(),
                    requestDto.getUsername(),
                    requestDto.getCurrency(),
                    false
            ), context);

            return response.getData();
        } catch (Exception ex) { // only operator exception then send to retry queue
            if (RetryPolicy.shouldRetry(RetryOrigin.BET_ROLLBACK, ex)) {
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
}
