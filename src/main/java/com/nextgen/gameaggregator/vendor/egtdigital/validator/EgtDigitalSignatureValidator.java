package com.nextgen.gameaggregator.vendor.egtdigital.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.Credentials;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.egtdigital.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EgtDigitalSignatureValidator extends AbstractVendorSignatureValidator {
    private static final String HEADER_CHECKSUM = "X-Checksum";
    private static final String HEADER_CHECKSUM_FIELDS = "X-Checksum-Fields";

    protected EgtDigitalSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                           VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.HMAC_SHA512_BASE64);
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        log.info("EGTDigital Request Body: \n" + formFields + "\n\nRequest Header: \n" + getHeaders(request) + "\n\nRaw Body: \n" + rawBody);
        AuthorizationHeaders headers = extractAuthorizationHeader(request);

        String username = formFields.get("playerId");
        if (username == null || username.isBlank()) {
            throw new SignatureValidationException("Missing username in form fields");
        }
        String appSecret = getCredentialValueByUsername(username, Credentials.HMAC);
        String dataToSign = buildDataToSign(headers.getChecksumFields(), formFields);
        checkSignature(headers.getChecksum(), dataToSign, appSecret);
        return ValidationResult.success();
    }

    private AuthorizationHeaders extractAuthorizationHeader(HttpServletRequest request) throws SignatureValidationException {
        String checkSum = request.getHeader(HEADER_CHECKSUM);
        String checkSumFields = request.getHeader(HEADER_CHECKSUM_FIELDS);

        if (checkSum == null || checkSum.isBlank() || checkSumFields == null || checkSumFields.isBlank()) {
            throw new SignatureValidationException("Missing Authorization header");
        }

        return new AuthorizationHeaders(checkSum, checkSumFields);
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return new VendorErrorResponse(HttpStatus.OK, new ErrorResponse(ResponseCodes.ERR_INTEGRITY_CHECK_FAILED));
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        return new VendorErrorResponse(HttpStatus.OK, new ErrorResponse(ResponseCodes.ERR_INTEGRITY_CHECK_FAILED));
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception) {
        return new VendorErrorResponse(HttpStatus.OK, new ErrorResponse(ResponseCodes.ERR_INVALID_PLAYER_ID));
    }

    private String buildDataToSign(String checksumFields, Map<String, String> formFields) throws SignatureValidationException {
        return Arrays.stream(checksumFields.split(","))
                .map(String::trim)
                .map(field -> getRequiredField(formFields, field))
                .collect(Collectors.joining(","));
    }

    private String getRequiredField(Map<String, String> formFields, String fieldName) throws SignatureValidationException {
        String value = formFields.get(fieldName);
        if (value == null || value.isBlank()) {
            throw new SignatureValidationException("Missing field for checksum: " + fieldName);
        }
        return value;
    }

    public String getHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        StringBuilder headersString = new StringBuilder();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headersString.append(headerName)
                    .append(":")
                    .append(headerValue)
                    .append("\n");
        }
        return headersString.toString();
    }
}
