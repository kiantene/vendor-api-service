package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
    // (OVI-2466). Production sets this via RollbackController.configure(...); the bare
    // constructor leaves it null, so it must be set explicitly here too.
    private final BetRollbackConfig config = new BetRollbackConfig().rollbackType(RollbackType.BY_BET);

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
}
