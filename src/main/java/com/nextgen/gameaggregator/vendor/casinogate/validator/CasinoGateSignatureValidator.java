package com.nextgen.gameaggregator.vendor.casinogate.validator;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.casinogate.config.CasinoGateConfig;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Credentials;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.casinogate.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.casinogate.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

@Component
public class CasinoGateSignatureValidator extends AbstractVendorSignatureValidator {
    private static final String BASIC_PREFIX = "Basic ";
    private static final String PROVIDER_PATH_PREFIX = "/api/v1/" + Endpoints.CLASS_NAME + "/v1/providers/";
    private static final String REQUEST_ATTR_VENDOR_PLAYER_USERNAME = "vendorPlayerUsername";
    private static final String REQUEST_ATTR_TOKEN = "token";

    protected CasinoGateSignatureValidator(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService, GameSessionDataService gameSessionDataService) {
        super(vendorPlayerDataService, vendorLineService, gameSessionDataService);
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        // Get username and password from the Authorization header.
        BasicCredentials basicCredentials = extractBasicCredentials(request);

        try {
            // Find the configured credential set by username.
            VendorCredentialAccessor credentialAccessor = getCredentialAccessorByKeyValue(
                    CasinoGateConfig.ID,
                    Credentials.USERNAME,
                    basicCredentials.username()
            );

            // Get expected values from vendor line credentials.
            String expectedUsername = credentialAccessor.getValue(Credentials.USERNAME);
            String expectedPassword = credentialAccessor.getValue(Credentials.PASSWORD);
            String expectedPlatformId = credentialAccessor.getValue(Credentials.PLATFORM_ID);
            String requestPlatformId = getPlatformId(request);

            // Validate Basic Auth username/password and the platformId from URL.
            if (!secureEquals(basicCredentials.username(), expectedUsername)
                    || !secureEquals(basicCredentials.password(), expectedPassword)
                    || !secureEquals(requestPlatformId, expectedPlatformId)) {
                throw invalidCredentials();
            }
        } catch (InternalConfigurationException ex) {
            throw invalidCredentials();
        }

        String token = formFields.get(REQUEST_ATTR_TOKEN);
        if (token == null || token.isBlank()) {
            throw new SignatureValidationException("Missing token", new InvalidRequestException("Missing token"));
        }

        GameSession gameSession;
        try {
            gameSession = getGameSessionByToken(token);
        } catch (GameSessionExpiredException ex) {
            throw new SignatureValidationException("Game session expired", ex);
        }

        return ValidationResult.success(Map.of(
                REQUEST_ATTR_VENDOR_PLAYER_USERNAME, gameSession.getVendorPlayerUsername()
        ));
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        if (exception.getCause() instanceof InvalidRequestException) {
            return errorResponse(ResponseCodes.INVALID_REQUEST);
        }

        if (exception.getCause() instanceof GameSessionExpiredException) {
            return errorResponse(ResponseCodes.SESSION_NOT_FOUND);
        }

        return unauthorizedResponse();
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception) {
        return unauthorizedResponse();
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    @Override
    public String getVendorClassName() {
        return Endpoints.CLASS_NAME;
    }

    private BasicCredentials extractBasicCredentials(HttpServletRequest request) {

        // Read Basic Auth header from request.
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BASIC_PREFIX)) {
            throw invalidCredentials();
        }

        // Remove "Basic " and keep only the Base64 value.
        String encodedCredentials = authorization.substring(BASIC_PREFIX.length()).trim();
        String decodedCredentials;

        // Decode Base64 value into "username:password".
        try {
            decodedCredentials = new String(
                    Base64.getDecoder().decode(encodedCredentials),
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException ex) {
            throw invalidCredentials();
        }

        // Split into username and password.
        String[] credentials = decodedCredentials.split(":", 2);
        String username = credentials[0];
        String password = credentials.length > 1 ? credentials[1] : "";

        // Username and password are both required.
        if (username.isBlank() || password.isBlank()) {
            throw invalidCredentials();
        }

        // Return parsed Basic Auth credentials.
        return new BasicCredentials(username, password);
    }

    private String getPlatformId(HttpServletRequest request) {
        // Example path: /api/v1/casinogate/v1/providers/zenith-qa/wallet
        String path = request.getRequestURI();

        // Remove application context path if the app is deployed with one.
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        // Make sure this request is using CasinoGate provider callback path.
        if (!path.startsWith(PROVIDER_PATH_PREFIX)) {
            throw invalidCredentials();
        }

        // After removing the prefix, the first path segment is platformId.
        // Example: zenith-qa/wallet -> zenith-qa
        String platformId = path.substring(PROVIDER_PATH_PREFIX.length()).split("/")[0];

        // Platform id cannot be empty.
        if (platformId.isBlank()) {
            throw invalidCredentials();
        }

        return platformId;
    }

    private boolean secureEquals(String actual, String expected) {
        // Use constant-time comparison for credentials.
        if (actual == null || expected == null) {
            return false;
        }

        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    private SignatureValidationException invalidCredentials() {
        return new SignatureValidationException("Invalid Basic Auth credentials");
    }

    private VendorErrorResponse unauthorizedResponse() {
        return errorResponse(ResponseCodes.UNAUTHORIZED);
    }

    private VendorErrorResponse errorResponse(ResponseCodes responseCode) {
        ErrorResponse response = ErrorResponse.builder()
                .code(responseCode.getCode())
                .description(responseCode.getDescription())
                .build();

        return new VendorErrorResponse(responseCode.getHttpStatus(), response);
    }

    private record BasicCredentials(String username, String password) {
    }
}
