package com.nextgen.gameaggregator.vendor.crystal.api.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.common.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.common.VendorSignatureValidator;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.signature.HmacSha256SignatureStrategy;
import com.nextgen.gameaggregator.core.signature.SignatureStrategy;
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
public class CrystalSignatureValidator extends AbstractVendorSignatureValidator implements VendorSignatureValidator {
    private static final String HEADER_AUTHORIZATION = "X-Signature";

    protected CrystalSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                        VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService);
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public void validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String signatureHeader = extractAuthorizationHeader(request);
        String username = formFields.get("playerId");
        if (username == null || username.isBlank()) {
            throw new SignatureValidationException("Missing username in form fields");
        }

        String appSecret = getCredentialValue(username, Credentials.SECRET_KEY);
        String compactJsonBody = this.convertToJson(rawBody);
        SignatureStrategy signatureStrategy = new HmacSha256SignatureStrategy();
        String computedSignature = signatureStrategy.sign(compactJsonBody, appSecret);
        if (!computedSignature.equalsIgnoreCase(signatureHeader)) {
            throw new SignatureValidationException("Invalid signature");
        }
    }

    private String convertToJson(String jsonBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object jsonObject = mapper.readValue(jsonBody, Object.class);
            return mapper.writeValueAsString(jsonObject).replaceAll("\\s+", "");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }
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
                .data(ErrorResponse.Data.builder().build())
                .error((ErrorResponse.Error.builder()
                        .code(String.valueOf(ResponseCodes.PLAYER_NOT_FOUND))
                        .message(ResponseCodes.PLAYER_NOT_FOUND.message))
                        .build())
                .build();
        return new VendorErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, response);
    }
}
