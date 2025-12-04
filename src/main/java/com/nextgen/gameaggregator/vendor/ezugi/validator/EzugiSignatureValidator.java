package com.nextgen.gameaggregator.vendor.ezugi.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class EzugiSignatureValidator extends AbstractVendorSignatureValidator {
    private static final String HEADER_HASH = "hash";
    private static final String OPERATOR_ID = "operatorId";
    private static final String HASH_KEY = "hashKey";

    protected EzugiSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                      VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.HMAC_SHA256_BASE64);
    }

    @Override
    public String getVendorClassName() {
        return Vendors.EZUGI.getClassName();
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String hash = extractHash(request);
        String secret = getSecretKey(formFields);
        checkSignature(hash, rawBody, secret);
        return ValidationResult.success();
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(ResponseCodes.GENERAL_ERROR, "Invalid Hash");
        return new VendorErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse);
    }

    private String extractHash(HttpServletRequest request) {
        String auth = request.getHeader(HEADER_HASH);
        if (!StringUtils.hasText(auth)) {
            throw new SignatureValidationException("Missing hash header");
        }
        return auth;
    }

    private String getSecretKey(Map<String, String> formFields) {
        String operatorId = formFields.get(OPERATOR_ID);
        VendorCredentialAccessor credentialsMap = getCredentialAccessorByKeyValue(24, Credentials.OPERATOR_ID, operatorId);
        if (credentialsMap == null) {
            throw new SignatureValidationException("Vendor credentials not found for operatorId: " + operatorId);
        }
        return credentialsMap.getValue(HASH_KEY);
    }
}
