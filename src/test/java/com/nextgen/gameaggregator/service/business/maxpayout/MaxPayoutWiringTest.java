package com.nextgen.gameaggregator.service.business.maxpayout;

import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the OVI-2391 capped-amount pipeline that {@code AgentMaxPayoutServiceTest} does not cover:
 * {@code RoundTxn.of} copy → {@code TxnDelta.finalizeSuccess} carry → {@code GameRound.computeCappedTotals}
 * read-back. (The couchbase sub-doc write in {@code GameRoundRepository.applyTxnDelta} is infra and
 * needs an integration test.)
 */
class MaxPayoutWiringTest {

    private static final BigDecimal BET = new BigDecimal("2167");
    private static final BigDecimal VENDOR_WIN = new BigDecimal("2100");
    private static final BigDecimal CAPPED_WIN = new BigDecimal("2000");

    private GameTransaction cappedResultTxn() {
        GameTransaction txn = new GameTransaction();
        txn.setType(TxnType.RESULT);
        txn.setStatus(TxnStatus.SUCCESS);
        txn.setState(GameRoundState.SETTLED);
        txn.setWinAmount(VENDOR_WIN);
        txn.setCappedWinAmount(CAPPED_WIN);
        return txn;
    }

    /** RoundTxn.of must copy the capped fields, or they never reach the persisted slice. */
    @Test
    void roundTxnOf_copiesCappedFields() {
        RoundTxn slice = RoundTxn.of(cappedResultTxn());

        assertEquals(0, slice.getCappedWinAmount().compareTo(CAPPED_WIN));
        assertEquals(0, slice.cappedWinAmountOrVendor().compareTo(CAPPED_WIN));
    }

    /** GameTransaction.copy() must carry the capped fields (regression: manual field enumeration). */
    @Test
    void gameTransactionCopy_preservesCappedFields() {
        GameTransaction copy = cappedResultTxn().copy();

        assertEquals(0, copy.getCappedWinAmount().compareTo(CAPPED_WIN));
    }

    /** finalizeSuccess must carry the capped deltas so applyTxnDelta can persist them. */
    @Test
    void txnDelta_finalizeSuccess_carriesCappedDeltas() {
        TxnDelta d = TxnDelta.finalizeSuccess(
                "doc", 0, TxnType.RESULT, "gaBet", new BigDecimal("135"),
                BigDecimal.ZERO, VENDOR_WIN, BigDecimal.ZERO,
                CAPPED_WIN, BigDecimal.ZERO,          // cappedWin, cappedJackpot
                BET, "08:50:31.025", true, true);

        assertTrue(d.cappedWinDelta().isPresent());
        assertEquals(0, d.cappedWinDelta().get().compareTo(CAPPED_WIN));
        assertTrue(d.cappedJackpotDelta().isPresent());
    }

    /** Uncapped txns leave the capped deltas empty (no sub-doc write → slice stays null). */
    @Test
    void txnDelta_finalizeSuccess_emptyWhenUncapped() {
        TxnDelta d = TxnDelta.finalizeSuccess(
                "doc", 0, TxnType.RESULT, "gaBet", new BigDecimal("135"),
                BigDecimal.ZERO, VENDOR_WIN, BigDecimal.ZERO,
                null, null,                          // no cap
                BET, "08:50:31.025", true, true);

        assertTrue(d.cappedWinDelta().isEmpty());
        assertTrue(d.cappedJackpotDelta().isEmpty());
    }

    /** End-to-end (minus couchbase): capped slice → round totals split capped vs vendor. */
    @Test
    void computeCappedTotals_readsBackCappedSlice() {
        RoundTxn bet = new RoundTxn();
        bet.setType(TxnType.BET);
        bet.setStatus(TxnStatus.SUCCESS);
        bet.setState(GameRoundState.UNSETTLED);
        bet.setBetAmount(BET);

        RoundTxn result = RoundTxn.of(cappedResultTxn()); // winAmount=2100, cappedWinAmount=2000

        GameRound round = new GameRound();
        round.setTransactions(List.of(bet, result));

        assertEquals(0, round.computeCappedTotals().win().compareTo(CAPPED_WIN)); // 2000
        assertEquals(0, round.computeTotals().win().compareTo(VENDOR_WIN));       // 2100
        // win_loss: capped -167 vs vendor -67
        assertEquals(0, round.computeCappedTotals().win().subtract(round.computeCappedTotals().bet())
                .compareTo(new BigDecimal("-167")));
    }
}
