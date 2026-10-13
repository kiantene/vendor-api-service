package com.nextgen.gameaggregator.service.business;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.data.GameRoundDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameRoundServiceTest {

    @Mock
    private GameRoundDataService data;

    @InjectMocks
    private GameRoundService gameRoundService;

    private GameTransaction txnWithId(String className, String transactionId) {
        GameTransaction txn = new GameTransaction();
        txn.setClassName(className);
        txn.setUsername("player1");
        txn.setRoundId("round-1");
        txn.setTransactionId(transactionId);
        return txn;
    }

    private RoundTxn roundTxnMatching(GameTransaction txn) {
        RoundTxn roundTxn = new RoundTxn();
        roundTxn.setId(txn.getId());
        return roundTxn;
    }

    // The common case: idx is already known, so no round fetch is needed at all.
    @Test
    void updateRoundTxn_withAssignedIdx_updatesDirectlyWithoutFetchingRound() {
        GameTransaction txn = txnWithId("gpkv2", "txn-1");
        txn.setIdx(2);

        gameRoundService.updateRoundTxn(txn, GameRoundState.REFUNDED);

        verify(data).updateTxn(txn.getRoundDocId(), 2, Map.of("state", GameRoundState.REFUNDED));
        verify(data, never()).findById(any());
    }

    // A bet still in flight has no idx backfilled onto its standalone doc yet, but the round
    // already has its slot (appended by markSent before the operator was ever called) -
    // resolve the real index by matching the txn's own deterministic id.
    @Test
    void updateRoundTxn_withUnassignedIdx_resolvesRealIndexFromRound() {
        GameTransaction txn = txnWithId("gpkv2", "txn-1");
        GameTransaction otherTxn = txnWithId("gpkv2", "txn-0");

        GameRound round = GameRound.of("gpkv2", "player1", "round-1");
        round.setTransactions(List.of(roundTxnMatching(otherTxn), roundTxnMatching(txn)));

        when(data.findById(txn.getRoundDocId())).thenReturn(KvDoc.of(txn.getRoundDocId(), round));

        gameRoundService.updateRoundTxn(txn, GameRoundState.REFUNDED);

        verify(data).updateTxn(txn.getRoundDocId(), 1, Map.of("state", GameRoundState.REFUNDED));
    }

    // The round exists but genuinely has no matching slot (shouldn't happen in practice,
    // since markSent always appends before the operator is called) - log and skip rather
    // than throwing or guessing at an index.
    @Test
    void updateRoundTxn_withUnassignedIdx_noMatchInRound_skipsWithoutThrowing() {
        GameTransaction txn = txnWithId("gpkv2", "txn-1");
        GameTransaction otherTxn = txnWithId("gpkv2", "txn-0");

        GameRound round = GameRound.of("gpkv2", "player1", "round-1");
        round.setTransactions(List.of(roundTxnMatching(otherTxn)));

        when(data.findById(txn.getRoundDocId())).thenReturn(KvDoc.of(txn.getRoundDocId(), round));

        assertDoesNotThrow(() -> gameRoundService.updateRoundTxn(txn, GameRoundState.REFUNDED));

        verify(data, never()).updateTxn(eq(txn.getRoundDocId()), anyInt(), any());
    }

    // The round document itself doesn't exist at all - same safe skip, no exception.
    @Test
    void updateRoundTxn_withUnassignedIdx_roundNotFound_skipsWithoutThrowing() {
        GameTransaction txn = txnWithId("gpkv2", "txn-1");

        when(data.findById(txn.getRoundDocId())).thenReturn(null);

        assertDoesNotThrow(() -> gameRoundService.updateRoundTxn(txn, GameRoundState.REFUNDED));

        verify(data, never()).updateTxn(eq(txn.getRoundDocId()), anyInt(), any());
    }
}
