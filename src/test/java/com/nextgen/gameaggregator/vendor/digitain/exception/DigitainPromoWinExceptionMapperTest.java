package com.nextgen.gameaggregator.vendor.digitain.exception;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.digitain.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DigitainPromoWinExceptionMapperTest {

    private final DigitainPromoWinExceptionMapper mapper = new DigitainPromoWinExceptionMapper();

    /**
     * The promo-payout duplicate guard carries a {@link RequestIdempotentLog}, never a GameTransaction.
     * Reading getTransaction() here threw an NPE out of the mapper and the vendor saw a 500 instead of err 8.
     */
    @Test
    void replayingATxidReportsTransactionAlreadyExists() {
        RequestIdempotentLog log = new RequestIdempotentLog();
        log.setTransactionId("40439c1a12cbe8c68df1");
        log.setCurrency("EUR");
        log.setBalance(new BigDecimal("1987.216543"));

        VendorErrorResponse response = mapper.onDuplicateRequest(
                new DuplicateRequestException("digitain:payout:40439c1a12cbe8c68df1 already processed", log));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getErr()).isEqualTo(ResponseCode.TRANSACTION_ALREADY_EXISTS.code);
        assertThat(body.getMsg()).isEqualTo(ResponseCode.TRANSACTION_ALREADY_EXISTS.description);
        assertThat(body.getTxid()).isEqualTo("40439c1a12cbe8c68df1");
        assertThat(body.getBln()).isEqualTo(new BigDecimal("1987.2165"));
    }

    /** Nothing to read back from: still err 8, with a zero balance rather than an NPE. */
    @Test
    void replayingATxidWithNoRecordedStateStillReportsTransactionAlreadyExists() {
        VendorErrorResponse response = mapper.onDuplicateRequest(
                new DuplicateRequestException("already processed"));

        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getErr()).isEqualTo(ResponseCode.TRANSACTION_ALREADY_EXISTS.code);
        assertThat(body.getTxid()).isNull();
        assertThat(body.getBln()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
