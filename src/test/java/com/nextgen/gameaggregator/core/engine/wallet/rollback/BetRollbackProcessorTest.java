package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import com.nextgen.gameaggregator.core.service.LegacyCleanupService;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiAdapter;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.BetTransactionHistoryProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetRollbackProcessorTest {

    @Mock private BetRollbackContextEnricher enricher;
    @Mock private GameRoundService gameRoundService;
    @Mock private GameTransactionService gameTransactionService;
    @Mock private ClientResponseValidator clientResponseValidator;
    @Mock private OperatorApiAdapter operatorApiAdapter;
    @Mock private BetHistoryProducer betHistoryProducer;
    @Mock private RetryQueueService retryQueueService;
    @Mock private LegacyCleanupService legacyCleanupService;
    @Mock private VendorConfigService vendorConfigService;
    @Mock private BetTransactionHistoryProducer betTransactionHistoryProducer;

    @Mock private BetRollbackContext context;
    // Real config, not a mock: RollbackPolicy requires an explicitly-defined rollbackType
    // (OVI-2466) or it rejects up front - BetRollbackConfig's own constructor leaves it
    // null, so every test in this class needs it set explicitly to reach the decision
    // being tested (BY_BET, since processRollbackByBet is the by-bet-scoped rollback).
    private final BetRollbackConfig config = new BetRollbackConfig()
            .rollbackType(com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType.BY_BET);

    @InjectMocks private BetRollbackProcessor processor;

    // A duplicate rollback on an already-refunded bet is a no-op: the player's funds
    // are already back, so we must return the round's last-known balance (kept current
    // by OVI-2519), not a hardcoded ZERO.
    @Test
    void processRollbackByBet_noOp_returnsRoundLastBalance() {
        GameRound round = GameRound.of("koolbet", "player1", "round-1");
        round.setLastBalance(new BigDecimal("88.00"));

        GameTransaction betTxn = new GameTransaction();
        betTxn.setState(GameRoundState.REFUNDED); // -> decision.isNoOp()
        betTxn.setClassName("koolbet");
        betTxn.setUsername("player1");
        betTxn.setRoundId("round-1"); // getRoundDocId() == round.getId()
        betTxn.setCurrency("THB");

        GameTransaction rollbackTxn = new GameTransaction();

        when(gameTransactionService.getOrThrow(any())).thenReturn(betTxn);
        when(gameRoundService.getOrThrow(round.getId())).thenReturn(round);

        PlayerBalanceData result = processor.processRollbackByBet(context, rollbackTxn, config);

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("88.00")));
        assertEquals("player1", result.getUsername());
        assertEquals("THB", result.getCurrency());
        // no-op must not touch the operator wallet
        verify(operatorApiAdapter, never()).execute(any());
    }

    /**
     * callToOperator() is private — invoked via reflection since these tests target the exact
     * catch block that changed (ONEAPI-492/618 v2 counterpart: operator failures used to be
     * silently swallowed with no trace left on the request's own httpRequestLog).
     */
    private void invokeCallToOperator(BetRollbackContext ctx, GameRound round, String gaBetId,
                                      String transactionId, GameTransaction betTxn) throws Exception {
        Method method = BetRollbackProcessor.class.getDeclaredMethod("callToOperator",
                BetRollbackContext.class, GameRound.class, String.class, String.class, GameTransaction.class);
        method.setAccessible(true);
        method.invoke(processor, ctx, round, gaBetId, transactionId, betTxn);
    }

    private GameRound gameRoundWithAgentMeta() {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(11540);
        agentMeta.setGameCode("SPB_aviator");
        agentMeta.setUsername("player1");
        agentMeta.setCurrency("INR");

        GameRound round = GameRound.of("spribe", "player1", "round-1");
        round.setAgentMeta(agentMeta);
        return round;
    }

    /**
     * Wires operatorApiAdapter.execute()/throwIfError() to succeed at the transport level, then
     * has clientResponseValidator.validate() throw — mirroring the real ONEAPI-618 trigger
     * (an operator response with no "data" field, caught by ClientResponseValidator.validate()).
     */
    private void stubOperatorCallToFailValidation() throws InvalidOperatorResponseException {
        OperatorApiRequest apiRequest = mock(OperatorApiRequest.class);
        when(apiRequest.getPartitionKey()).thenReturn("partition-1");
        when(operatorApiAdapter.toApiRequest(any(WalletRollbackDto.class), any())).thenReturn(apiRequest);

        ApiResult apiResult = mock(ApiResult.class);
        when(operatorApiAdapter.execute(any())).thenReturn(apiResult);
        when(apiResult.throwIfError()).thenReturn(apiResult);

        ClientApiResponse response = mock(ClientApiResponse.class);
        when(apiResult.parseTo(ClientApiResponse.class)).thenReturn(response);

        doThrow(new InvalidOperatorResponseException("Data is empty"))
                .when(clientResponseValidator).validate(any(), any(), any());

        when(retryQueueService.enqueue(any(), any())).thenReturn(Mono.empty());
    }

    @Test
    void callToOperator_operatorFailure_recordsErrorOnHttpRequestLog() throws Exception {
        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-1");
        when(context.getHttpRequestLog()).thenReturn(httpRequestLog);

        stubOperatorCallToFailValidation();

        invokeCallToOperator(context, gameRoundWithAgentMeta(), "gaBetId1", "txn1", null);

        assertEquals(LogContextService.STATUS_ERROR, httpRequestLog.getStatus());
        assertEquals(
                "com.nextgen.gameaggregator.exception.InvalidOperatorResponseException: Data is empty",
                httpRequestLog.getErrorMessage()
        );
    }

    @Test
    void callToOperator_operatorFailure_stillQueuesRetryAndReturnsDefaultBalance() throws Exception {
        // The vendor-facing "return default success + retry in background" behavior is
        // intentional and must be untouched by the observability fix.
        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-1");
        when(context.getHttpRequestLog()).thenReturn(httpRequestLog);

        stubOperatorCallToFailValidation();

        invokeCallToOperator(context, gameRoundWithAgentMeta(), "gaBetId1", "txn1", null);

        verify(retryQueueService).enqueue(any(), any());
    }

    @Test
    void callToOperator_noHttpRequestLogOnContext_doesNotThrow() throws Exception {
        // Defensive: a context without an httpRequestLog (e.g. async paths) must not NPE.
        when(context.getHttpRequestLog()).thenReturn(null);

        stubOperatorCallToFailValidation();

        invokeCallToOperator(context, gameRoundWithAgentMeta(), "gaBetId1", "txn1", null);
    }

    private GameRound roundPendingRollback(String username, BigDecimal lastBalance) {
        GameRound round = GameRound.of("gpkv2", username, "round-1");
        round.setLastBalance(lastBalance);
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(1);
        agentMeta.setUsername(username);
        agentMeta.setCurrency("MXN");
        round.setAgentMeta(agentMeta);
        return round;
    }

    private GameTransaction betTxnStillPending(String username, GameRound round) {
        GameTransaction betTxn = new GameTransaction();
        betTxn.setStatus(TxnStatus.SENT); // still waiting on the operator -> RollbackPolicy defers
        betTxn.setClassName("gpkv2");
        betTxn.setUsername(username);
        // getRoundDocId() rebuilds className::username::roundId itself - pass the plain
        // roundId (matching how GameRound.of(...) was given it below), not round.getId().
        betTxn.setRoundId("round-1");
        betTxn.setCurrency("MXN");
        return betTxn;
    }

    private void stubDeferralPlumbing() {
        // deferRollbackToRetryQueue() builds a real HttpCallSpec from whatever
        // operatorApiAdapter.toApiRequest(...) returns, so it needs a non-null request
        // with a non-null partition key (RetryHelper hashes it); the queue call itself
        // needs a non-null Mono to subscribe to.
        OperatorApiRequest apiRequest = mock(OperatorApiRequest.class);
        when(apiRequest.getPartitionKey()).thenReturn("player1");
        when(operatorApiAdapter.toApiRequest(any(WalletRollbackDto.class), any())).thenReturn(apiRequest);
        when(retryQueueService.enqueueWithDelay(any(), any(), any())).thenReturn(Mono.empty());
    }

    // OVI-1925 (Option A, per Sim Junyi): when the operator call itself is deferred to
    // retry, GA's own bookkeeping (bet-history + markRefunded) must still happen up
    // front - not wait for the deferred operator call to eventually succeed or fail.
    @Test
    void processRollbackByBet_defer_recordsBetHistoryAndRefundUpfront() {
        GameRound round = roundPendingRollback("player1", new BigDecimal("50.00"));
        GameTransaction betTxn = betTxnStillPending("player1", round);
        GameTransaction rollbackTxn = new GameTransaction();

        when(gameTransactionService.getOrThrow(any())).thenReturn(betTxn);
        when(gameRoundService.getOrThrow(round.getId())).thenReturn(round);
        stubDeferralPlumbing();

        processor.processRollbackByBet(context, rollbackTxn, config);

        verify(betHistoryProducer).publishBetHistoryForRollback(context, round, betTxn);
        verify(gameTransactionService).markRefunded(betTxn);
        verify(gameTransactionService).markSent(rollbackTxn, null);
    }

    // The operator call itself must still go through the retry queue (never called
    // synchronously), and the vendor gets a forced-success response using the round's
    // last-known balance - the same "force success" shape as a synchronous operator
    // failure, just decided up front instead of after a failed attempt.
    @Test
    void processRollbackByBet_defer_enqueuesRetryAndReturnsForcedSuccessBalance() {
        GameRound round = roundPendingRollback("player1", new BigDecimal("50.00"));
        GameTransaction betTxn = betTxnStillPending("player1", round);
        GameTransaction rollbackTxn = new GameTransaction();

        when(gameTransactionService.getOrThrow(any())).thenReturn(betTxn);
        when(gameRoundService.getOrThrow(round.getId())).thenReturn(round);
        stubDeferralPlumbing();

        PlayerBalanceData result = processor.processRollbackByBet(context, rollbackTxn, config);

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("50.00")));
        assertEquals("player1", result.getUsername());
        verify(operatorApiAdapter, never()).execute(any());
        verify(retryQueueService).enqueueWithDelay(any(), eq(RetryOrigin.BET_ROLLBACK), eq(Duration.ofSeconds(5)));
    }

    // onAfterSendBetRollback() (markRollback + legacy cleanup) depends on the real
    // operator response, which doesn't exist yet when deferred - it must only run once
    // the deferred call actually completes, never speculatively at defer time.
    @Test
    void processRollbackByBet_defer_doesNotFinalizeRollbackTxnBeforeOperatorResponds() {
        GameRound round = roundPendingRollback("player1", new BigDecimal("50.00"));
        GameTransaction betTxn = betTxnStillPending("player1", round);
        GameTransaction rollbackTxn = new GameTransaction();

        when(gameTransactionService.getOrThrow(any())).thenReturn(betTxn);
        when(gameRoundService.getOrThrow(round.getId())).thenReturn(round);
        stubDeferralPlumbing();

        processor.processRollbackByBet(context, rollbackTxn, config);

        verify(gameTransactionService, never()).markRollback(any(), any(), any());
    }

    // A bare .subscribe() with no error consumer would let a failed enqueue crash the
    // reactive chain silently with no trace; processRollbackByBet() itself must still
    // return its normal forced-success response regardless (the bookkeeping already
    // happened before the enqueue was even attempted).
    @Test
    void processRollbackByBet_defer_enqueueFailure_doesNotPropagateOrThrow() {
        GameRound round = roundPendingRollback("player1", new BigDecimal("50.00"));
        GameTransaction betTxn = betTxnStillPending("player1", round);
        GameTransaction rollbackTxn = new GameTransaction();

        when(gameTransactionService.getOrThrow(any())).thenReturn(betTxn);
        when(gameRoundService.getOrThrow(round.getId())).thenReturn(round);

        OperatorApiRequest apiRequest = mock(OperatorApiRequest.class);
        when(apiRequest.getPartitionKey()).thenReturn("player1");
        when(operatorApiAdapter.toApiRequest(any(WalletRollbackDto.class), any())).thenReturn(apiRequest);
        when(retryQueueService.enqueueWithDelay(any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("couchbase write failed")));

        PlayerBalanceData result = processor.processRollbackByBet(context, rollbackTxn, config);

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("50.00")));
        // bookkeeping already committed before the (failed) enqueue attempt
        verify(gameTransactionService).markRefunded(betTxn);
    }
}
