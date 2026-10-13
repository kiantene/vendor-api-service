package com.nextgen.gameaggregator.vendor.jdb.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.BetAlreadySettledException;
import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.exception.BetResultRejectedException;
import com.nextgen.gameaggregator.core.exception.DuplicateBetException;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyRefundedException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies the wire-code mapping for JDB exceptions surfaced by the new debit/credit framework.
 * The aspect-driven {@code @VendorExceptionHandler} dispatch routes runtime exceptions to these
 * methods; this test asserts the response codes match the legacy JDB catch-chain behavior.
 */
class JdbExceptionMapperTest {

    private final JdbExceptionMapper mapper = new JdbExceptionMapper();

    private static String statusOf(VendorErrorResponse response) {
        assertInstanceOf(CommonVo.class, response.getBody());
        return ((CommonVo) response.getBody()).getStatus();
    }

    @Test
    void onInsufficientBalance_returnsInsufficientBalanceCode() {
        VendorErrorResponse response = mapper.onInsufficientBalance(new InsufficientBalanceException());
        assertEquals(ResponseCode.INSUFFICIENT_BALANCE, statusOf(response));
    }

    @Test
    void onBetNotAllowed_returnsFailed() {
        VendorErrorResponse response = mapper.onBetNotAllowed(new BetNotAllowedException());
        assertEquals(ResponseCode.FAILED, statusOf(response));
    }

    @Test
    void onDuplicateBet_returnsDuplicateTransaction() {
        // Defensive — wrapper's returnSuccessOnDuplicate handles idempotent replays upstream,
        // so this only fires when a vendor opts out. Code path must still return the right wire code.
        VendorErrorResponse response = mapper.onDuplicateBet(new DuplicateBetException());
        assertEquals(ResponseCode.DUPLICATE_TRANSACTION, statusOf(response));
    }

    @Test
    void onDuplicateRequest_returnsDuplicateTransaction() {
        VendorErrorResponse response = mapper.onDuplicateRequest(new DuplicateRequestException());
        assertEquals(ResponseCode.DUPLICATE_TRANSACTION, statusOf(response));
    }

    @Test
    void onRollbackNotAllowed_betAlreadySettled_returnsCannotCancel() {
        // Action 11 naming a settled debit — legacy CancelBetService answers CANNOT_CANCEL.
        VendorErrorResponse response = mapper.onRollbackNotAllowed(
                new RollbackNotAllowedException(null, new BetAlreadySettledException("settled")));
        assertEquals(ResponseCode.CANNOT_CANCEL, statusOf(response));
    }

    @Test
    void onRollbackNotAllowed_alreadyRefunded_returnsCannotCancel() {
        // Action 11 naming a debit already cancelled under another transferId.
        VendorErrorResponse response = mapper.onRollbackNotAllowed(
                new RollbackNotAllowedException(null, new RoundAlreadyRefundedException("refunded")));
        assertEquals(ResponseCode.CANNOT_CANCEL, statusOf(response));
    }

    @Test
    void onBetResultRejected_settleOfAConsumedDebit_returnsFailed() {
        // Action 10 naming a debit already settled or cancelled. JDB has no "cannot settle" code.
        VendorErrorResponse response = mapper.onBetResultRejected(
                new BetResultRejectedException(null, new BetAlreadySettledException("settled"), null));
        assertEquals(ResponseCode.FAILED, statusOf(response));
    }

    @Test
    void onRollbackNotAllowed_otherCause_returnsFailed() {
        VendorErrorResponse response = mapper.onRollbackNotAllowed(
                new RollbackNotAllowedException(null, new BetNotFoundException("missing")));
        assertEquals(ResponseCode.FAILED, statusOf(response));
    }

    @Test
    void onBetResultRejected_returnsFailed() {
        VendorErrorResponse response = mapper.onBetResultRejected(
                new BetResultRejectedException(null, new RuntimeException("rejected"), null));
        assertEquals(ResponseCode.FAILED, statusOf(response));
    }

    @Test
    void onInvalidRequestError_returnsInvalidRequestParameter() {
        VendorErrorResponse response = mapper.onInvalidRequestError(new InvalidRequestException("bad"));
        assertEquals(ResponseCode.INVALID_REQUEST_PARAMETER, statusOf(response));
    }

    @Test
    void onInternalError_returnsFailed() {
        VendorErrorResponse response = mapper.onInternalError(new InternalServerException("kaboom"));
        assertEquals(ResponseCode.FAILED, statusOf(response));
    }

    @Test
    void onInternalConfigurationError_returnsFailed() {
        VendorErrorResponse response = mapper.onInternalConfigurationError(new InternalConfigurationException("config"));
        assertEquals(ResponseCode.FAILED, statusOf(response));
    }
}
