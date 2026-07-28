package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.service.LegacyCleanupService;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.core.webclient.OperatorApiAdapter;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.BetTransactionHistoryProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    // (OVI-2466). The real constructor defaults it to BY_BET, so the no-op path is reached
    // whether or not that validation is present on this branch.
    private final BetRollbackConfig config = new BetRollbackConfig();

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
}
