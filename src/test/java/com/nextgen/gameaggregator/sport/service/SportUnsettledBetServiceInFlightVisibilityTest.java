package com.nextgen.gameaggregator.sport.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.repository.SportUnsettledBetRepository;

/**
 * ONEAPI-974 — who is allowed to see a write-ahead record.
 *
 * <p>The write-ahead makes the bet visible before the operator debit is attempted. Every existing reader
 * of sport_unsettled_bet predates that state and treats "a record exists" as "the bet is done" — a settle
 * landing in the window would pay out a win on a stake that may never be debited.
 *
 * <p>So the two lookups deliberately disagree about the same document: the shared lookup reports it as
 * not-found, and only the refund path's lookup can see it.
 */
class SportUnsettledBetServiceInFlightVisibilityTest {

    private static final String USER = "32msz0fb9xe";
    private static final String BET_ID = "1349703";
    private static final String KEY = USER + '_' + BET_ID;

    /** Short bound so the refund lookup does not sleep through the real 8s. */
    private static final long TEST_MAX_WAIT_MILLIS = 300L;

    private SportUnsettledBetRepository repo;
    private SportUnsettledBetService service;

    @BeforeEach
    void setUp() {
        repo = mock(SportUnsettledBetRepository.class);
        service = new SportUnsettledBetService(repo, TEST_MAX_WAIT_MILLIS);
    }

    private SportUnsettledBet betWithOperatorStatus(Integer operatorStatus) {
        SportUnsettledBet bet = new SportUnsettledBet();
        bet.setVendorPlayerUsername(USER);
        bet.setVendorBetId(BET_ID);
        bet.setStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        bet.setOperatorStatus(operatorStatus);
        return bet;
    }

    @Test
    void sharedLookup_inFlightBet_reportsNotFound_soSettleBehavesAsBefore() {
        SportUnsettledBet inFlight = betWithOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        when(repo.findById(KEY)).thenReturn(Optional.of(inFlight));

        // Settle, confirm and the Pinnacle paths all come through here. They must keep seeing exactly what
        // they saw before the write-ahead existed: nothing.
        assertThrows(BetNotFoundException.class,
                () -> service.getByVendorPlayerUsernameAndVendorBetId(USER, BET_ID),
                "A bet whose operator debit has not been attempted is not a usable bet");
    }

    @Test
    void refundLookup_sameInFlightBet_stillSeesIt() throws BetNotFoundException {
        SportUnsettledBet inFlight = betWithOperatorStatus(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
        when(repo.findById(KEY)).thenReturn(Optional.of(inFlight));

        // The deliberate disagreement: the refund path is the one reader built for this state, so it must
        // still get the bet and decide for itself (it waits, then refunds — an indeterminate debit must be
        // refunded). If this ever matches the shared lookup, ONEAPI-974's NPE comes straight back.
        assertSame(inFlight, service.awaitPlaceBetOutcome(USER, BET_ID).orElseThrow());
    }

    @Test
    void sharedLookup_confirmedBet_isReturnedNormally() throws BetNotFoundException {
        SportUnsettledBet confirmed = betWithOperatorStatus(ResponseCodes.Status.SC_OK.code);
        when(repo.findById(KEY)).thenReturn(Optional.of(confirmed));

        assertSame(confirmed, service.getByVendorPlayerUsernameAndVendorBetId(USER, BET_ID));
    }

    @Test
    void sharedLookup_placeBetFailedAtOperator_isStillReturned() throws BetNotFoundException {
        // Only the in-flight state is hidden. A placebet that FINISHED and failed is a real bet with a known
        // (or indeterminate) debit outcome — settle and refund must still be able to act on it, exactly as
        // they could before this change.
        SportUnsettledBet timedOut = betWithOperatorStatus(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);
        when(repo.findById(KEY)).thenReturn(Optional.of(timedOut));

        assertSame(timedOut, service.getByVendorPlayerUsernameAndVendorBetId(USER, BET_ID));
    }
}
