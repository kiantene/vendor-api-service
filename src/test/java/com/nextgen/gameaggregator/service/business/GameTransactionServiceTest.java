package com.nextgen.gameaggregator.service.business;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class GameTransactionServiceTest {

    @Mock
    private GameTransactionDataService txnDataService;
    @Mock
    private GameRoundService gameRoundService;

    @InjectMocks
    private GameTransactionService gameTransactionService;

    // OVI-2519: after a successful rollback the game round is finalized in a single
    // round mutation — the rollback slot is marked COMPLETED and lastBalance is
    // refreshed with the operator's post-rollback (refunded) balance.
    @Test
    void markRollback_finalizesRound_withRefundedBalance() {
        GameRound round = GameRound.of("koolbet", "player1", "round-1");
        GameTransaction rollbackTxn = new GameTransaction();
        BigDecimal refundedBalance = new BigDecimal("123.45");

        gameTransactionService.markRollback(round, rollbackTxn, refundedBalance);

        verify(gameRoundService).updateTxnStateAndBalance(rollbackTxn, GameRoundState.COMPLETED, refundedBalance);
        verify(txnDataService).updateStatus(rollbackTxn, refundedBalance, TxnStatus.SUCCESS);
        assertEquals(GameRoundState.COMPLETED, rollbackTxn.getState());
    }

    // A missing operator balance is forwarded as null so the round mutation still
    // finalizes the slot state but leaves lastBalance untouched (repo-level guard),
    // never clobbering the last known balance.
    @Test
    void markRollback_forwardsNullBalance_toFinalizeRound() {
        GameRound round = GameRound.of("koolbet", "player1", "round-1");
        GameTransaction rollbackTxn = new GameTransaction();

        gameTransactionService.markRollback(round, rollbackTxn, null);

        verify(gameRoundService).updateTxnStateAndBalance(rollbackTxn, GameRoundState.COMPLETED, null);
    }

    // markRefunded() always marks the standalone txn doc and delegates the round-slot
    // annotation to GameRoundService.updateRoundTxn() - that method resolves idx itself
    // (even when it's still unassigned on a bet still in flight), so this class no longer
    // needs to branch on idx at all; see GameRoundServiceTest for that resolution logic.
    @Test
    void markRefunded_marksStandaloneDocAndDelegatesRoundStateUpdate() {
        GameTransaction txn = new GameTransaction();
        txn.setIdx(0);

        gameTransactionService.markRefunded(txn);

        verify(txnDataService).updateToRefunded(txn.getId());
        verify(gameRoundService).updateRoundTxn(txn, GameRoundState.REFUNDED);
    }

    // ONEAPI-2065: settling a bet whose transactionId differs from its vendorBetId must
    // also settle the alias doc, or the alias stays UNSETTLED with no TTL forever.
    @Test
    void markBetAndAliasSettled_settlesBetAndAlias_whenAliasExists() {
        RoundTxn bet = roundBet("evoplay::BET::txn-1", "vendor-bet-1");

        gameTransactionService.markBetAndAliasSettled(bet, "evoplay", 1000L);

        verify(txnDataService).updateToSettled("evoplay::BET::txn-1", 1000L);
        verify(txnDataService).updateToSettled("evoplay::BET::vendor-bet-1", 1000L);
    }

    @Test
    void markBetAndAliasSettled_settlesOnlyBet_whenNoAlias() {
        RoundTxn bet = roundBet("evoplay::BET::same-id", "same-id");

        gameTransactionService.markBetAndAliasSettled(bet, "evoplay", 1000L);

        verify(txnDataService).updateToSettled("evoplay::BET::same-id", 1000L);
        verifyNoMoreInteractions(txnDataService);
    }

    // The alias is created only after markSent and expires after 3 days; a missing alias
    // must not fail settlement of the real bet.
    @Test
    void markBetAndAliasSettled_ignoresMissingAlias() {
        RoundTxn bet = roundBet("evoplay::BET::txn-1", "vendor-bet-1");
        lenient().doThrow(new DocumentNotFoundException(null))
                .when(txnDataService).updateToSettled("evoplay::BET::vendor-bet-1", 1000L);

        assertDoesNotThrow(() -> gameTransactionService.markBetAndAliasSettled(bet, "evoplay", 1000L));

        verify(txnDataService).updateToSettled("evoplay::BET::txn-1", 1000L);
    }

    private static RoundTxn roundBet(String id, String vendorBetId) {
        RoundTxn bet = new RoundTxn();
        bet.setId(id);
        bet.setVendorBetId(vendorBetId);
        return bet;
    }
}
