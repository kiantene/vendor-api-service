package com.nextgen.gameaggregator.vendor.spribe.security;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.spribe.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spribe.validator.SpribeSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignatureValidationTest {

    private static final String CLIENT_ID     = "test-client-001";
    private static final String CLIENT_SECRET = "super-secret-key";
    private static final int    VENDOR_LINE_ID = 42;

    @Mock private VendorPlayerDataService vendorPlayerDataService;
    @Mock private VendorLineService vendorLineService;

    private SpribeSignatureValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SpribeSignatureValidator(vendorPlayerDataService, vendorLineService);
    }

    private static String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void stubCredentials() throws Exception {
        VendorLineCredential secretCred = new VendorLineCredential();
        secretCred.setValue(CLIENT_SECRET);
        doReturn(VENDOR_LINE_ID).when(vendorLineService)
                .getVendorLineIdByNameAndValue(Credentials.OPERATOR, CLIENT_ID);
        when(vendorLineService.mapCredentialsByName(VENDOR_LINE_ID))
                .thenReturn(Map.of(Credentials.TOKEN, secretCred));
    }

    private MockHttpServletRequest buildRequest(String clientId, String clientTs, String signature) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/deposit");
        req.addHeader("X-Spribe-Client-ID", clientId);
        req.addHeader("X-Spribe-Client-TS", clientTs);
        req.addHeader("X-Spribe-Client-Signature", signature);
        return req;
    }

    // -----------------------------------------------------------------------
    // Valid Signature (Scenario 10.1)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Valid Signature (Scenario 10.1)")
    class ValidSignature {

        // Scenario 10.1
        @Test
        @DisplayName("10.1: valid X-Spribe-Client-Signature — passes validation; ValidationResult.success()")
        void validSignature_passesValidation() throws Exception { // stubCredentials() declares checked exception
            String clientTs = "1716000000";
            String path     = "/api/v1/spribe/deposit";
            String body     = "{\"user_id\":\"player_001\",\"currency\":\"USD\"}";
            String payload  = clientTs + path + body;
            String sig      = hmacSha256Hex(payload, CLIENT_SECRET);

            stubCredentials();
            MockHttpServletRequest request = buildRequest(CLIENT_ID, clientTs, sig);

            ValidationResult result = validator.validate(request, Map.of(), body);

            assertThat(result.valid()).isTrue();
            assertThat(result.isSkipped()).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // Invalid Signature (Scenario 3.3 / 10.2)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Invalid Signature (Scenario 3.3 / 10.2)")
    class InvalidSignature {

        // Scenario 3.3 / 10.2
        //
        // Monitor-only mode: a wrong signature is logged but NOT rejected — validate()
        // returns ValidationResult.skipped() and the request is allowed through, until we
        // confirm zero false positives in prod. When enforcement is re-enabled (uncomment
        // the throw in SpribeSignatureValidator), restore the assertThatThrownBy(...)
        // expectation on SignatureValidationException("Signature mismatch").
        @Test
        @DisplayName("3.3: invalid X-Spribe-Client-Signature — monitor-only: logged and skipped, not rejected")
        void invalidSignature_monitorOnly_isSkippedNotRejected() throws Exception {
            String clientTs      = "1716000000";
            String body          = "{\"user_id\":\"player_001\",\"currency\":\"USD\"}";
            String wrongSig      = "deadbeef0000000000000000000000000000000000000000000000000000cafe";

            stubCredentials();
            MockHttpServletRequest request = buildRequest(CLIENT_ID, clientTs, wrongSig);

            ValidationResult result = validator.validate(request, Map.of(), body);

            assertThat(result.isSkipped()).isTrue();
            assertThat(result.valid()).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // Missing Headers — skip validation (Scenario 10.3)
    //
    // When Spribe omits any of the auth headers we cannot recompute the HMAC,
    // so the validator returns ValidationResult.skipped() and the auth filter
    // lets the request proceed. This restores prod's pre-validator behaviour
    // for unauthenticated callbacks (e.g. freebet deposits).
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Missing Headers — skip (Scenario 10.3)")
    class MissingHeaders {

        @Test
        @DisplayName("10.3a: missing X-Spribe-Client-ID — validation skipped")
        void missingClientId_skipsValidation() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/v2/deposit");
            req.addHeader("X-Spribe-Client-TS", "1716000000");
            req.addHeader("X-Spribe-Client-Signature", "somesig");

            ValidationResult result = validator.validate(req, Map.of(), "{}");

            assertThat(result.isSkipped()).isTrue();
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("10.3b: missing X-Spribe-Client-TS — validation skipped")
        void missingClientTs_skipsValidation() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/v2/deposit");
            req.addHeader("X-Spribe-Client-ID", CLIENT_ID);
            req.addHeader("X-Spribe-Client-Signature", "somesig");

            ValidationResult result = validator.validate(req, Map.of(), "{}");

            assertThat(result.isSkipped()).isTrue();
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("10.3c: missing X-Spribe-Client-Signature — validation skipped")
        void missingSignature_skipsValidation() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/v2/deposit");
            req.addHeader("X-Spribe-Client-ID", CLIENT_ID);
            req.addHeader("X-Spribe-Client-TS", "1716000000");

            ValidationResult result = validator.validate(req, Map.of(), "{}");

            assertThat(result.isSkipped()).isTrue();
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("10.3d: all three headers missing — validation skipped")
        void allHeadersMissing_skipsValidation() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/v2/deposit");

            ValidationResult result = validator.validate(req, Map.of(), "{}");

            assertThat(result.isSkipped()).isTrue();
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("10.3e: blank X-Spribe-Client-ID — validation skipped")
        void blankClientId_skipsValidation() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/v2/deposit");
            req.addHeader("X-Spribe-Client-ID", "   ");
            req.addHeader("X-Spribe-Client-TS", "1716000000");
            req.addHeader("X-Spribe-Client-Signature", "somesig");

            ValidationResult result = validator.validate(req, Map.of(), "{}");

            assertThat(result.isSkipped()).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // shouldValidate — v1 vs v2 path routing
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("shouldValidate — v1 vs v2 path routing")
    class ShouldValidate {

        @Test
        @DisplayName("v1 deposit path — shouldValidate returns false (no signature check)")
        void v1DepositPath_shouldNotValidate() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/deposit");
            assertThat(validator.shouldValidate(req, req.getRequestURI())).isFalse();
        }

        @Test
        @DisplayName("v2 deposit path — shouldValidate returns true (signature check enabled)")
        void v2DepositPath_shouldValidate() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/v2/deposit");
            assertThat(validator.shouldValidate(req, req.getRequestURI())).isTrue();
        }

        @Test
        @DisplayName("v1 withdraw path — shouldValidate returns false")
        void v1WithdrawPath_shouldNotValidate() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/withdraw");
            assertThat(validator.shouldValidate(req, req.getRequestURI())).isFalse();
        }

        @Test
        @DisplayName("v2 withdraw path — shouldValidate returns true")
        void v2WithdrawPath_shouldValidate() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/spribe/v2/withdraw");
            assertThat(validator.shouldValidate(req, req.getRequestURI())).isTrue();
        }
    }
}
