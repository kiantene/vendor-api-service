package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.api.ApiResult;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.retry.RetryHelper;
import com.nextgen.gameaggregator.core.retry.RetryPolicy;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import com.nextgen.gameaggregator.core.service.LegacyCleanupService;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiAdapter;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
class BetRollbackProcessor {
    private final BetRollbackContextEnricher enricher;
    private final GameRoundService gameRoundService;
    private final GameTransactionService gameTransactionService;
    private final ClientResponseValidator clientResponseValidator;
    private final OperatorApiAdapter operatorApiAdapter;
    private final BetHistoryProducer betHistoryProducer;
    private final RetryQueueService retryQueueService;
    private final LegacyCleanupService legacyCleanupService;

    public PlayerBalanceData processRollbackByBet(BetRollbackContext context, GameTransaction rollbackTxn, BetRollbackConfig config) {
        // There is a possibility that the alias txn is not created yet if an error is encountered before markSent is called
        GameTransaction betTxn = gameTransactionService.getOrThrow(rollbackTxn.getRollbackId());

        RollbackDecision decision = RollbackPolicy.decide(betTxn, config);
        decision.throwIfRejected(context);

        if (decision.isNoOp()) {
            return PlayerBalanceData.getDefault(betTxn.getUsername(), betTxn.getCurrency());
        }

        GameRound round = gameRoundService.getOrThrow(betTxn.getRoundDocId());

        enricher.enrichByGameRound(context, round, rollbackTxn, betTxn);

        if (decision.isDefered()) {
            deferRollbackToRetryQueue(context, round, betTxn);
            return PlayerBalanceData.getDefaultWithBalance(betTxn.getUsername(), betTxn.getCurrency(), round.getLastBalanceWithDefault());
        }

        onBeforeSendBetRollback(context, round, betTxn, rollbackTxn);

        PlayerBalanceData balanceData = callToOperator(context, round, betTxn.getGaBetId(), betTxn.getTransactionId());

        onAfterSendBetRollback(context, round, betTxn, rollbackTxn, balanceData);

        return balanceData;
    }

    private void deferRollbackToRetryQueue(BetRollbackContext context, GameRound round, GameTransaction betTxn) {
        WalletRollbackDto requestDto = mapToClientRequest(context, round, betTxn.getGaBetId(), betTxn.getTransactionId());
        OperatorApiRequest apiRequest = operatorApiAdapter.toApiRequest(requestDto, round.getAgentMeta().getAgentId());

        retryQueueService
                .enqueueWithDelay(RetryHelper.toHttpCallSpec(apiRequest), RetryOrigin.BET_ROLLBACK, Duration.ofSeconds(5))
                .subscribe();
    }

    public PlayerBalanceData processRollbackByRound(BetRollbackContext context, GameTransaction rollbackTxn, BetRollbackConfig config) {
        GameRound round = gameRoundService.getOrThrow(rollbackTxn.getRoundDocId());

        RollbackDecision decision = RollbackPolicy.decide(round, config);
        decision.throwIfRejected(context);

        if (decision.isNoOp()) {
            return PlayerBalanceData.getDefault(round.getUsername(), round.getCurrency());
        }

        enricher.enrichByGameRound(context, round, rollbackTxn);

        //TODO: to revisit if we want standardise the bet history for round rollback by using transaction id from bet txn
        // Get First Bet or BetNResult
        Optional<GameTransaction> firstBetTxn =
                round.getTransactions().stream()
                        .filter(t -> t.isBet() || t.isBetNResult())
                        .filter(RoundTxn::isSuccess)
                        .findFirst()
                        .flatMap(txn -> gameTransactionService.get(txn.getId()));

        betHistoryProducer.publishBetHistoryForRollbackByRound(context, round, firstBetTxn, rollbackTxn);

        var byGaBetId = round.getTransactions().stream()
                .filter(RoundTxn::isSuccessfulBetOrResult)
                .filter(t -> t.getGaBetId() != null)
                .collect(Collectors.groupingBy(RoundTxn::getGaBetId));

        gameTransactionService.markSent(rollbackTxn, null);

        PlayerBalanceData balanceData = new PlayerBalanceData();
        for (Map.Entry<String, List<RoundTxn>> entry : byGaBetId.entrySet()) {
            String gaBetId = entry.getKey();

            // 1. Call once per gaBetId -> overwrite each time
            // Use the original bet txn's id as external id for rollback.
            // TODO: to revisit later if we want to use a different external id for round rollback
            // TODO: Exception Handling for Rollback by Round?
            balanceData = callToOperator(context, round, gaBetId, context.getTransactionId());

            // 2. Update every txn in this group
            entry.getValue().forEach(t -> {
                gameTransactionService.markRefunded(t.getId());
                legacyCleanupService.cleanup(round, t.getVendorBetId(), context.getVendorGameId(), context.getVendorPlayerId());
            });
        }

        BigDecimal balance = Optional.ofNullable(balanceData.getBalance()).orElse(round.getLastBalance());
        gameTransactionService.markRollback(round, rollbackTxn, balance);

        return balanceData;
    }

    private PlayerBalanceData callToOperator(BetRollbackContext context, GameRound round, String gaBetId, String transactionId) {
        // any exception thrown here is considered internal error
        WalletRollbackDto requestDto = mapToClientRequest(context, round, gaBetId, transactionId);
        OperatorApiRequest apiRequest = operatorApiAdapter.toApiRequest(requestDto, round.getAgentMeta().getAgentId());

        try {
            ApiResult apiResult = operatorApiAdapter.execute(apiRequest);
            apiResult.throwIfError();
            ClientApiResponse response = apiResult.parseTo(ClientApiResponse.class);

            clientResponseValidator.validate(response, new ClientResponseValidator.RequestRecord(
                    "wallet.rollback",
                    requestDto.getTraceId(),
                    requestDto.getUsername(),
                    requestDto.getCurrency(),
                    false
            ), context);

            // Translate Back to Vendor Amount
            return response.getData().toVendorView(
                    round.getUsername(),
                    round.getCurrency(),
                    context.getToVendorRate()
            );

        } catch (Exception ex) { // only operator exception then send to retry queue
            if (RetryPolicy.shouldRetry(RetryOrigin.BET_ROLLBACK, ex)) {
                retryQueueService
                        .enqueue(RetryHelper.toHttpCallSpec(apiRequest), RetryOrigin.BET_ROLLBACK)
                        .subscribe();
            }
        }
        /**
         * Return Last Known Round Balance
         * If Previous Bet fails too, Last Known Round Balance will be 0
         */
        return PlayerBalanceData.getDefaultWithBalance(
                round.getAgentMeta().getUsername(),
                round.getAgentMeta().getCurrency(),
                Optional.ofNullable(round.getLastBalance()).orElse(BigDecimal.ZERO)
        );
    }

    private WalletRollbackDto mapToClientRequest(BetRollbackContext context, GameRound round, String betId, String transactionId) {
        WalletRollbackDto dto = new WalletRollbackDto();

        dto.setTraceId(context.getTraceId());
        dto.setTransactionId(UuidUtil.newUuidV7String());
        dto.setBetId(betId);
        dto.setExternalTransactionId(transactionId);
        dto.setRoundId(round.getRoundId());
        dto.setGameCode(round.getAgentMeta().getGameCode());
        dto.setUsername(round.getAgentMeta().getUsername());
        dto.setCurrency(round.getAgentMeta().getCurrency());
        dto.setTimestamp(context.getTimestamp());

        return dto;
    }

    private void onBeforeSendBetRollback(BetRollbackContext context,
                                         GameRound round,
                                         GameTransaction betTxn,
                                         GameTransaction rollbackTxn) {

        betHistoryProducer.publishBetHistoryForRollback(context, round, betTxn);

        // Update bet txn status to refunded
        gameTransactionService.markRefunded(betTxn);
        gameTransactionService.markSent(rollbackTxn, null);
        legacyCleanupService.cleanup(round, betTxn.getVendorBetId(), context.getVendorGameId(), context.getVendorPlayerId());
    }

    private void onAfterSendBetRollback(BetRollbackContext context,
                                        GameRound round,
                                        GameTransaction betTxn,
                                        GameTransaction rollbackTxn,
                                        PlayerBalanceData balanceData) {

        // we save the original balance from operator
        gameTransactionService.markRollback(round, rollbackTxn, balanceData.getBalance());
    }
}