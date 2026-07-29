package com.nextgen.gameaggregator.vendor.evolutionv2.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.BetResultRejectedException;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.core.exception.PlayerDisabledException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evolutionv2.constant.EndPoints;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evolution v2 promo-payout integration.
 */
class EvolutionExceptionMapperTest {

    private final EvolutionExceptionMapper mapper = new EvolutionExceptionMapper();

    @Test
    void getVendorClassName_returnsEvolution() {
        assertThat(mapper.getVendorClassName()).isEqualTo(EndPoints.CLASS_NAME);
    }

    @Test
    void onGameSessionExpired_returnsInvalidSid() {
        assertResponse(mapper.onGameSessionExpired(new GameSessionExpiredException()), ResponseCode.INVALID_SID);
    }

    @Test
    void onGameTerminated_returnsTemporaryError() {
        assertResponse(mapper.onGameTerminated(new GameTerminatedException()), ResponseCode.TEMPORARY_ERROR);
    }

    @Test
    void onInsufficientBalance_returnsInsufficientFunds() {
        assertResponse(mapper.onInsufficientBalance(new InsufficientBalanceException()), ResponseCode.INSUFFICIENT_FUNDS);
    }

    @Test
    void onPlayerDisabled_returnsAccountLocked() {
        assertResponse(mapper.onPlayerDisabled(new PlayerDisabledException(null, "disabled")), ResponseCode.ACCOUNT_LOCKED);
    }

    @Test
    void onBetNotAllowed_returnsInvalidParameter() {
        assertResponse(mapper.onBetNotAllowed(new BetNotAllowedException()), ResponseCode.INVALID_PARAMETER);
    }

    @Test
    void onDuplicateRequest_returnsIdempotentOk() {
        assertResponse(mapper.onDuplicateRequest(new DuplicateRequestException("duplicate")), ResponseCode.OK);
    }

    @Test
    void onBetResultRejected_returnsTemporaryError() {
        assertResponse(
                mapper.onBetResultRejected(new BetResultRejectedException(null, new RuntimeException("rejected"), null)),
                ResponseCode.TEMPORARY_ERROR
        );
    }

    @Test
    void onInvalidRequestError_returnsInvalidParameter() {
        assertResponse(mapper.onInvalidRequestError(new InvalidRequestException("invalid")), ResponseCode.INVALID_PARAMETER);
    }

    @Test
    void onInternalConfigurationError_returnsTemporaryError() {
        assertResponse(
                mapper.onInternalConfigurationError(new InternalConfigurationException("missing configuration")),
                ResponseCode.TEMPORARY_ERROR
        );
    }

    @Test
    void onInternalError_returnsUnknownError() {
        assertResponse(mapper.onInternalError(new InternalServerException("failure")), ResponseCode.UNKNOWN_ERROR);
    }

    private void assertResponse(VendorErrorResponse errorResponse, ResponseCode responseCode) {
        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(errorResponse.getBody()).isInstanceOf(ResponseVo.class);

        ResponseVo response = (ResponseVo) errorResponse.getBody();
        assertThat(response.getResponseCode()).isEqualTo(responseCode);
        assertThat(response.getStatus()).isEqualTo(responseCode.status);
    }
}
