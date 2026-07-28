package com.nextgen.gameaggregator.service.business;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

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
}
