package com.nextgen.gameaggregator.vendor.evolutionv2.validator;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.service.VendorService;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evolutionv2.constant.EndPoints;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Evolution v2 promo-payout integration.
 */
@ExtendWith(MockitoExtension.class)
class EvolutionAuthTokenValidatorTest {

    @Mock private VendorService vendorService;
    @Mock private HttpServletRequest request;

    private EvolutionAuthTokenValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EvolutionAuthTokenValidator(vendorService);
    }

    @Test
    void shouldValidate_onlyPromoPayoutEndpoint() {
        assertThat(validator.shouldValidate(request, "/api/v1/netent/promo_payout")).isTrue();
        assertThat(validator.shouldValidate(request, "/api/v1/netent/credit")).isFalse();
    }

    @Test
    void validate_acceptsValidSession() throws Exception {
        when(vendorService.preCheckGameSessionToken("sid-123")).thenReturn(session("sid-123", "player123", "USD", 1));

        var result = validator.validate(request, fields(), "{}");

        assertThat(result.valid()).isTrue();
    }

    @Test
    void validate_rejectsMissingRequiredFields() {
        Map<String, String> fields = new HashMap<>(fields());
        fields.remove("sid");

        assertThatThrownBy(() -> validator.validate(request, fields, "{}"))
                .isInstanceOfSatisfying(EvolutionCallbackValidationException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.INVALID_PARAMETER));
    }

    @Test
    void validate_rejectsTerminatedSession() throws Exception {
        when(vendorService.preCheckGameSessionToken("sid-123")).thenReturn(session("sid-123", "player123", "USD", 0));

        assertThatThrownBy(() -> validator.validate(request, fields(), "{}"))
                .isInstanceOfSatisfying(EvolutionCallbackValidationException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.INVALID_SID));
    }

    @Test
    void validate_rejectsVendorTokenMismatch() throws Exception {
        when(vendorService.preCheckGameSessionToken("sid-123")).thenReturn(session("other-token", "player123", "USD", 1));

        assertThatThrownBy(() -> validator.validate(request, fields(), "{}"))
                .isInstanceOfSatisfying(EvolutionCallbackValidationException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.INVALID_SID));
    }

    @Test
    void validate_rejectsUsernameOrCurrencyMismatch() throws Exception {
        when(vendorService.preCheckGameSessionToken("sid-123")).thenReturn(session("sid-123", "someone-else", "USD", 1));

        assertThatThrownBy(() -> validator.validate(request, fields(), "{}"))
                .isInstanceOfSatisfying(EvolutionCallbackValidationException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.INVALID_PARAMETER));
    }

    @Test
    void validate_mapsInvalidSidOnAuthenticationFailure() throws Exception {
        when(vendorService.preCheckGameSessionToken("sid-123")).thenThrow(new AuthenticationException());

        assertThatThrownBy(() -> validator.validate(request, fields(), "{}"))
                .isInstanceOfSatisfying(EvolutionCallbackValidationException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.INVALID_SID));
    }

    @Test
    void onInvalidSignature_returnsEvolutionErrorAndEchoesUuid() {
        var error = validator.onInvalidSignature(
                new EvolutionCallbackValidationException(ResponseCode.INVALID_SID, "request-123")
        );

        assertThat(error.getBody()).isInstanceOf(ResponseVo.class);
        ResponseVo response = (ResponseVo) error.getBody();
        assertThat(response.getResponseCode()).isEqualTo(ResponseCode.INVALID_SID);
        assertThat(response.getUuid()).isEqualTo("request-123");
        assertThat(validator.getVendorClassName()).isEqualTo(EndPoints.CLASS_NAME);
    }

    private GameSession session(String vendorToken, String username, String currency, Integer status) {
        GameSession gameSession = new GameSession();
        gameSession.setVendorToken(vendorToken);
        gameSession.setVendorPlayerUsername(username);
        gameSession.setVendorCurrencyCode(currency);
        gameSession.setStatus(status);
        return gameSession;
    }

    private Map<String, String> fields() {
        return Map.of(
                "userId", "player123",
                "sid", "sid-123",
                "currency", "USD",
                "uuid", "request-123"
        );
    }
}
