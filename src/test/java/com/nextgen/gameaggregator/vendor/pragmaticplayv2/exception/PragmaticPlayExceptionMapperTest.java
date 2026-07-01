package com.nextgen.gameaggregator.vendor.pragmaticplayv2.exception;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.freeround.FreeRoundPayoutResponse;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.spribe.exception.SpribeExceptionMapper;
import com.nextgen.gameaggregator.vendor.spribe.response.ErrorResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PragmaticPlayExceptionMapperTest {

    @Test
    void onDuplicateRequest_replaysTournamentPayoutResponseSnapshot() {
        DuplicateRequestException exception = duplicateRequestException();

        VendorErrorResponse errorResponse = new PragmaticPlayExceptionMapper().onDuplicateRequest(exception);

        assertThat(errorResponse.getBody()).isInstanceOfSatisfying(FreeRoundPayoutResponse.class, body -> {
            assertThat(body.getTransactionId()).isEqualTo("b6a17e1f3a594cf1bdef4b47");
            assertThat(body.getCurrency()).isEqualTo("PHP");
            assertThat(body.getCash()).isEqualByComparingTo("500428.54");
            assertThat(body.getBonus()).isZero();
            assertThat(body.getError()).isZero();
            assertThat(body.getDescription()).isEqualTo("Success");
        });
    }

    @Test
    void nonPragmaticPlayDuplicateRequestBehaviourIsUnchanged() {
        DuplicateRequestException exception = duplicateRequestException();

        VendorErrorResponse errorResponse = new SpribeExceptionMapper().onDuplicateRequest(exception);

        assertThat(errorResponse.getBody()).isEqualTo(
                new ErrorResponse(ErrorCodes.DUPLICATE_TRANSACTION.code, ErrorCodes.DUPLICATE_TRANSACTION.description)
        );
    }

    private DuplicateRequestException duplicateRequestException() {
        RequestIdempotentLog log = new RequestIdempotentLog();
        log.setTransactionId("b6a17e1f3a594cf1bdef4b47");
        log.setCurrency("PHP");
        log.setBalance(new BigDecimal("500428.54"));
        return new DuplicateRequestException("duplicate", log);
    }
}
