package com.nextgen.gameaggregator.vendor.casinogate.exception;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.casinogate.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class CasinoGateExceptionMapperTest {

    private final CasinoGateExceptionMapper mapper = new CasinoGateExceptionMapper();

    @Test
    void invalidRequestUsesCasinoGateResponseWithoutFieldErrors() {
        InvalidRequestException exception = new InvalidRequestException("invalid request");

        VendorErrorResponse response = mapper.onInvalidRequestError(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOfSatisfying(ErrorResponse.class, body -> {
            assertThat(body.getCode()).isEqualTo(400);
            assertThat(body.getDescription())
                    .isEqualTo("invalid request payload. ensure all required fields are provided");
        });
        assertThat(exception.isShowFieldErrors()).isFalse();
    }

    @Test
    void rollbackNotAllowedUsesInternalErrorResponse() {
        RollbackNotAllowedException exception = new RollbackNotAllowedException("Rollback rejected: Amount Mismatch.");

        VendorErrorResponse response = mapper.onRollbackNotAllowed(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOfSatisfying(ErrorResponse.class, body -> {
            assertThat(body.getCode()).isZero();
            assertThat(body.getDescription()).isEqualTo("something went wrong");
        });
    }
}
