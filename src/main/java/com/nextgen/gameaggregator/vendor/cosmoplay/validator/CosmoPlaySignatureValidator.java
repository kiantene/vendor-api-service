package com.nextgen.gameaggregator.vendor.cosmoplay.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.cosmoplay.config.CosmoPlayVendorConfig;
import com.nextgen.gameaggregator.vendor.cosmoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cosmoplay.entity.Player;
import com.nextgen.gameaggregator.vendor.cosmoplay.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.cosmoplay.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class CosmoPlaySignatureValidator extends AbstractVendorSignatureValidator {
    public static final String JWT_TOKEN_HEADER = "Authorizationv2";

    protected CosmoPlaySignatureValidator(
            VendorPlayerDataService vendorPlayerDataService,
            VendorLineService vendorLineService,
            GameSessionDataService gameSessionDataService
    ) {
        super(
                vendorPlayerDataService,
                vendorLineService,
                gameSessionDataService,
                SigningStrategyType.HMAC_SHA256_BASE64
        );
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception) {
        return ErrorResponse.of(ResponseCode.NOT_FOUND, "Invalid player: " + exception.getMessage());
    }

    @Override
    public String getVendorClassName() {
        return CosmoPlayVendorConfig.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return ErrorResponse.of(ResponseCode.UNAUTHORIZED, "Invalid signature");
    }

    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        String message = "Invalid request signature - " + exception.getMessage();

        if (exception.getCause() != null) {
            message += ". Caused by: " + exception.getCause().getMessage();
        }

        return ErrorResponse.of(ResponseCode.UNAUTHORIZED, message);
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String username;

        try {
            username = this.parseUsername(formFields);
        } catch (PlayerNotFoundException ex) {
            throw new SignatureValidationException(ex.getMessage(), ex);
        }

        Player player = Player.fromValidator(username);
        String jwtSecret = this.getCredentialValueByUsername(player.getId(), Credentials.JWT_SECRET).trim();
        String jwtToken = extractAuthorizationHeader(request);

        // Parse JWT manually
        String[] parts = jwtToken.split("\\.");
        if (parts.length != 3) {
            throw new SignatureValidationException("Invalid JWT format");
        }

        // Verify signature
        String headerPayload = parts[0] + "." + parts[1];

        // Remove Base64 padding
        String expectedSignature = base64UrlEncode(sign(headerPayload, jwtSecret));
        if (!parts[2].equals(expectedSignature)) {
            throw new SignatureValidationException("JWT signature mismatch");
        }

        return ValidationResult.success();
    }

    private String parseUsername(Map<String, String> formFields) throws PlayerNotFoundException {
        String username = formFields.get("PlayerID");

        if (username == null) {
            throw new PlayerNotFoundException("Missing PlayerID: The parameter is required.");
        }

        username = username.trim();
        if (username.isBlank()) {
            throw new PlayerNotFoundException("Invalid PlayerID: The value cannot be empty or blank.");
        }

        return username;
    }

    private String extractAuthorizationHeader(HttpServletRequest request) throws SignatureValidationException {
        String bearer = Optional.ofNullable(request.getHeader(JWT_TOKEN_HEADER))
                .map(String::trim)
                .orElse("");

        if (bearer.isBlank()) {
            throw new SignatureValidationException("Missing Authorization header");
        }

        if (!bearer.startsWith("Bearer ")) {
            throw new SignatureValidationException("Invalid Authorization header format");
        }

        return bearer.substring(7);
    }

    //Since we using base64 we need to encode to base64Url format
    private String base64UrlEncode(String input) {
        return input
                .replace("=", "")    // Remove padding
                .replace("+", "-")   // Replace + with -
                .replace("/", "_");  // Replace / with _
    }
}
