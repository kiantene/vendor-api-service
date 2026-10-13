package com.nextgen.gameaggregator.sport.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.repository.SportUnsettledBetRepository;

/**
 * ONEAPI-974 — SABA cancel racing an in-flight placebet.
 *
 * <p>The placebet now writes its bet ahead of the operator debit, so the refund path can see a bet whose
 * debit has not been attempted yet. awaitPlaceBetOutcome is what keeps the refund from overtaking the
 * debit, without ever dropping a refund that might be owed.
 */
class SportUnsettledBetServiceAwaitPlaceBetOutcomeTest {

    private static final String USER = "32msz0fb9xe";
    private static final String BET_ID = "1349703";
    private static final String KEY = USER + '_' + BET_ID;

    private SportUnsettledBetRepository repo;
    private SportUnsettledBetService service;

    /** Short bound so the deadline paths do not sleep through the real 8s in CI. */
    private static final long TEST_MAX_WAIT_MILLIS = 1_000L;

    @BeforeEach
    void setUp() {
        repo = mock(SportUnsettledBetRepository.class);
        service = new SportUnsettledBetService(repo, TEST_MAX_WAIT_MILLIS);
    }

    /**
     * status is 0 for BOTH an in-flight and a failed placebet (BetStatus.UNSETTLED and
     * SC_TRANSACTION_STILL_PROCESSING share the value 0), which is precisely why operatorStatus — not
     * status — is the discriminator under test.
     */
    private SportUnsettledBet betWithOperatorStatus(Integer operatorStatus) {
        SportUnsettledBet bet = new SportUnsettledBet();
        bet.setVendorPlayerUsername(USER);
        bet.setVendorBetId(BET_ID);
        bet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        bet.setOperatorStatus(operatorStatus);
        return bet;
    }

    @Test
    void placeBetAlreadyConfirmed_returnsWithoutWaiting() {
        SportUnsettledBet confirmed = betWithOperatorStatus(ResponseCodes.Status.SC_OK.code);
        when(repo.findById(KEY)).thenReturn(Optional.of(confirmed));

        assertSame(confirmed, service.awaitPlaceBetOutcome(USER, BET_ID).orElseThrow());

        // Exactly one probe: a finished placebet must never be polled for.
        verify(repo, times(1)).findById(KEY);
    }

    @Test
    void placeBetInFlight_waitsForTheDebitOutcomeBeforeRefunding() {
        SportUnsettledBet inFlight = betWithOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        SportUnsettledBet confirmed = betWithOperatorStatus(ResponseCodes.Status.SC_OK.code);

        when(repo.findById(KEY)).thenReturn(
                Optional.of(inFlight),
                Optional.of(inFlight),
                Optional.of(confirmed));

        assertSame(confirmed, service.awaitPlaceBetOutcome(USER, BET_ID).orElseThrow(),
                "The refund must act on the placebet's settled outcome, not on the write-ahead record");
        verify(repo, atLeast(3)).findById(KEY);
    }

    @Test
    void placeBetFailedAtOperator_isTerminal_soTheRefundIsNotDelayed() {
        // An operator timeout is terminal for the placebet but INDETERMINATE for the money: the operator may
        // still have committed the debit, so the refund must go out — immediately, with no polling.
        SportUnsettledBet timedOut = betWithOperatorStatus(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);
        when(repo.findById(KEY)).thenReturn(Optional.of(timedOut));

        assertSame(timedOut, service.awaitPlaceBetOutcome(USER, BET_ID).orElseThrow());
        verify(repo, times(1)).findById(KEY);
    }

    @Test
    void placeBetStillInFlightAtDeadline_stillReturnsTheBet_soTheRefundIsNeverDropped() {
        SportUnsettledBet inFlight = betWithOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        when(repo.findById(KEY)).thenReturn(Optional.of(inFlight));

        assertSame(inFlight, service.awaitPlaceBetOutcome(USER, BET_ID).orElseThrow(),
                "An indeterminate debit must still be refunded — never swallowed because we gave up waiting");
    }

    @Test
    void betNeverAppears_reportsAbsenceAsEmpty_notNull() {
        when(repo.findById(anyString())).thenReturn(Optional.empty());
        when(repo.findByVendorPlayerUsernameAndVendorBetId(anyString(), anyString())).thenReturn(null);

        // The old findByVendorPlayerUsernameAndVendorBetIdOrById returned a bare null here, and
        // SportRefundProcessor dereferenced it -> NullPointerException, refund lost with no retry log.
        // Absence is now explicit; SportRefundProcessor decides what it means, because a duplicate cancel
        // for an ALREADY REFUNDED bet also lands here (a successful refund deletes the unsettled bet).
        assertTrue(service.awaitPlaceBetOutcome(USER, BET_ID).isEmpty());
    }
}
