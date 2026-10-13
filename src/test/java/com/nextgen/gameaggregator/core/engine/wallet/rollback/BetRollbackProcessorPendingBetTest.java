package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
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
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.GameRoundDataService;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.BetTransactionHistoryProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for a pending bet that has no idx backfilled onto its standalone doc yet
 * (the operator hasn't responded): rolling it back used to unbox a null idx inside
 * markRefunded() and throw, losing the rollback entirely. Uses real GameTransactionService and
 * GameRoundService (only their *DataService leaves are mocked) so the actual markRefunded() ->
 * updateRoundTxn() -> round-slot-resolution chain runs end to end, not a mock standing in
 * for any part of it.
 */
@ExtendWith(MockitoExtension.class)
class BetRollbackProcessorPendingBetTest {

    @Mock private BetRollbackContextEnricher enricher;
    @Mock private GameRoundDataService gameRoundDataService;
    @Mock private GameTransactionDataService txnDataService;
    @Mock private ClientResponseValidator clientResponseValidator;
    @Mock private OperatorApiAdapter operatorApiAdapter;
    @Mock private BetHistoryProducer betHistoryProducer;
    @Mock private RetryQueueService retryQueueService;
    @Mock private LegacyCleanupService legacyCleanupService;
    @Mock private VendorConfigService vendorConfigService;
    @Mock private BetTransactionHistoryProducer betTransactionHistoryProducer;
    @Mock private BetRollbackContext context;

    private final BetRollbackConfig config = new BetRollbackConfig().rollbackType(RollbackType.BY_BET);

    private GameRoundService gameRoundService;
    private BetRollbackProcessor processor;

    @BeforeEach
    void setUp() {
        gameRoundService = new GameRoundService(gameRoundDataService);
        GameTransactionService realGameTransactionService = new GameTransactionService(txnDataService, gameRoundService);

        // The rollback txn's own round-append (markSent -> save() -> addNewTxnToRound) always
        // takes this branch in these tests, since it never already exists in the round -
        // return a plausible next slot, distinct from betTxn's own index 0.
        when(gameRoundDataService.appendTxn(any(), any())).thenReturn(1);

        processor = new BetRollbackProcessor(
                enricher,
                gameRoundService,
                realGameTransactionService,
                clientResponseValidator,
                operatorApiAdapter,
                betHistoryProducer,
                retryQueueService,
                legacyCleanupService,
                vendorConfigService,
                betTransactionHistoryProducer
        );
    }

    // betTxn already has a real slot in the round (appended by markSent before the operator was
    // ever called) - it's only the standalone doc's own idx field that hasn't been backfilled
    // yet (that only happens once the bet's own markSuccess/markError eventually runs).
    private GameRound roundPendingRollback(String username, BigDecimal lastBalance, GameTransaction betTxn) {
        GameRound round = GameRound.of("gpkv2", username, "round-1");
        round.setLastBalance(lastBalance);
        // mutable - addNewTxnToRound() appends rollbackTxn's own slot into this same list
        round.setTransactions(new ArrayList<>(List.of(RoundTxn.of(betTxn))));
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(1);
        agentMeta.setUsername(username);
        agentMeta.setCurrency("MXN");
        round.setAgentMeta(agentMeta);
        return round;
    }

    private GameTransaction betTxn(String username, TxnStatus status) {
        GameTransaction betTxn = new GameTransaction();
        betTxn.setStatus(status);
        betTxn.setClassName("gpkv2");
        betTxn.setUsername(username);
        betTxn.setRoundId("round-1");
        betTxn.setCurrency("MXN");
        betTxn.setTransactionId("bet-1");
        // idx intentionally left unassigned - not yet backfilled onto the standalone doc
        return betTxn;
    }

    // transactionId == vendorBetId so GameTransactionService.shouldCreateAliasTxn() (used by the
    // real markSent() this test exercises) takes its "no alias needed" branch - alias creation
    // itself isn't what's under test here. className/username/roundId must match betTxn's round
    // (getRoundDocId() is derived from them) or markSent()'s save() thinks this is a brand new,
    // unrelated round instead of appending into the one already stubbed via stubFindRound().
    private GameTransaction rollbackTxn(String username) {
        GameTransaction rollbackTxn = new GameTransaction();
        rollbackTxn.setClassName("gpkv2");
        rollbackTxn.setUsername(username);
        rollbackTxn.setRoundId("round-1");
        rollbackTxn.setTransactionId("rollback-1");
        rollbackTxn.setVendorBetId("rollback-1");
        return rollbackTxn;
    }

    private void stubFindBetTxn(GameTransaction betTxn) {
        when(txnDataService.findById(any())).thenReturn(betTxn);
    }

    private void stubFindRound(GameRound round) {
        when(gameRoundDataService.findById(round.getId())).thenReturn(KvDoc.of(round.getId(), round));
    }

    private void stubDeferralPlumbing() {
        OperatorApiRequest apiRequest = mock(OperatorApiRequest.class);
        when(apiRequest.getPartitionKey()).thenReturn("player1");
        when(operatorApiAdapter.toApiRequest(any(WalletRollbackDto.class), any())).thenReturn(apiRequest);
        when(retryQueueService.enqueueWithDelay(any(), any(), any())).thenReturn(Mono.empty());
    }

    private void stubImmediateOperatorSuccess(BigDecimal postRollbackBalance) {
        OperatorApiRequest apiRequest = mock(OperatorApiRequest.class);
        when(operatorApiAdapter.toApiRequest(any(WalletRollbackDto.class), any())).thenReturn(apiRequest);

        ApiResult apiResult = mock(ApiResult.class);
        when(operatorApiAdapter.execute(any())).thenReturn(apiResult);
        when(apiResult.throwIfError()).thenReturn(apiResult);

        ClientApiResponse response = new ClientApiResponse();
        response.setData(PlayerBalanceData.ofVendorView("player1", "MXN", postRollbackBalance));
        when(apiResult.parseTo(ClientApiResponse.class)).thenReturn(response);
    }

    // TC1/TC5: a bet still waiting on the operator (status SENT) has no idx backfilled onto its
    // standalone doc yet. Before the first fix, onBeforeSendBetRollback's markRefunded() NPE'd
    // here instead of deferring. Before the follow-up fix, it silently skipped annotating the
    // round slot at all. Now it must resolve the real slot and mark it REFUNDED.
    @Test
    void processRollbackByBet_pendingBetWithUnassignedIdx_defersAndMarksRoundSlotRefunded() {
        GameTransaction betTxn = betTxn("player1", TxnStatus.SENT);
        GameRound round = roundPendingRollback("player1", new BigDecimal("50.00"), betTxn);
        GameTransaction rollbackTxn = rollbackTxn("player1");

        stubFindBetTxn(betTxn);
        stubFindRound(round);
        stubDeferralPlumbing();

        PlayerBalanceData result = assertDoesNotThrow(() ->
                processor.processRollbackByBet(context, rollbackTxn, config));

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("50.00")));
        verify(betHistoryProducer).publishBetHistoryForRollback(context, round, betTxn);
        // the round slot IS found and marked REFUNDED, at the resolved index (0 - betTxn is the
        // only entry in this round's transactions list)
        verify(gameRoundDataService).updateTxn(round.getId(), 0, Map.of("state", GameRoundState.REFUNDED));
        verify(operatorApiAdapter, never()).execute(any());
        verify(retryQueueService).enqueueWithDelay(any(), eq(RetryOrigin.BET_ROLLBACK), eq(Duration.ofSeconds(5)));
    }

    // TC3-style: a bet that already failed at the operator is immediately rollback-eligible
    // (RollbackPolicy allows ERROR/TIMEOUT, no defer) rather than going through the retry
    // queue. If its idx is also still unassigned (e.g. it errored before updateStatus backfilled
    // it), the same resolve-from-round fix must hold on this immediate-call path too.
    @Test
    void processRollbackByBet_failedBetWithUnassignedIdx_callsOperatorImmediatelyAndMarksRoundSlotRefunded() {
        GameTransaction betTxn = betTxn("player1", TxnStatus.ERROR);
        GameRound round = roundPendingRollback("player1", new BigDecimal("50.00"), betTxn);
        GameTransaction rollbackTxn = rollbackTxn("player1");

        stubFindBetTxn(betTxn);
        stubFindRound(round);
        stubImmediateOperatorSuccess(new BigDecimal("40.00"));

        PlayerBalanceData result = assertDoesNotThrow(() ->
                processor.processRollbackByBet(context, rollbackTxn, config));

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("40.00")));
        verify(gameRoundDataService).updateTxn(round.getId(), 0, Map.of("state", GameRoundState.REFUNDED));
        verify(operatorApiAdapter).execute(any());
        verify(retryQueueService, never()).enqueueWithDelay(any(), any(), any());
        // onAfterSendBetRollback still finalizes using rollbackTxn's own idx (assigned by
        // markSent, unaffected by betTxn's idx state), not betTxn's
        verify(gameRoundDataService).updateTxnStateAndBalance(eq(round.getId()), anyInt(), eq(GameRoundState.COMPLETED), any());
    }
}
