package com.nextgen.gameaggregator.vendor.crystal.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.crystal.constant.Credentials;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.crystal.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.crystal.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CrystalSignatureValidator extends AbstractVendorSignatureValidator {
    private static final String HEADER_AUTHORIZATION = "X-Signature";

    protected CrystalSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                        VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.HMAC_SHA256_HEX);
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String signatureHeader = extractAuthorizationHeader(request);
        String username = formFields.get("playerId");
        if (username == null || username.isBlank()) {
            throw new SignatureValidationException("Missing username in form fields");
        }

        String appSecret = getCredentialValueByUsername(username, Credentials.SECRET_KEY);
        String compactJsonBody = rawBody.replaceAll("\\s+", "");
        checkSignature(signatureHeader, compactJsonBody, appSecret);
        return ValidationResult.success();
    }

    private String extractAuthorizationHeader(HttpServletRequest request) throws SignatureValidationException {
        String signature = request.getHeader(HEADER_AUTHORIZATION);
        if (signature == null || signature.isBlank()) {
            throw new SignatureValidationException("Missing Authorization header");
        }
        return signature;
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .error(ErrorResponse.Error.of(ResponseCodes.INVALID_SIGNATURE))
                .build();
        return new VendorErrorResponse(ResponseCodes.INVALID_SIGNATURE.getHttpStatus(), response);
    }
}
