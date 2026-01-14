package com.nextgen.gameaggregator.vendor.lucky365.validator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.lucky365.constant.Credentials;
import com.nextgen.gameaggregator.vendor.lucky365.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.lucky365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.lucky365.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.lucky365.util.Lucky365Exception;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class Lucky365SignatureValidator extends AbstractVendorSignatureValidator {

    protected Lucky365SignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                         VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.MD5);
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String vendorSignature = requireField(this.resolveField("Signature", rawBody, formFields), "signature").toLowerCase(Locale.ROOT);
        String username = requireField(this.resolveField("LoginId", rawBody, formFields), "loginId");
        String id = requireField(this.resolveField("ID", rawBody, formFields), "id");
        String method = requireField(this.resolveField("Method", rawBody, formFields), "method");

        String secretKey = getCredentialValueByUsername(username, Credentials.SECRET_KEY);
        String sn = getCredentialValueByUsername(username, Credentials.SERIAL_NUM);
        String encryptString = id + method + sn + username + secretKey;
        String signature = sign(encryptString, "");

        checkSecret(vendorSignature, signature);
        return ValidationResult.success();
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        if (Lucky365Exception.isListResponse()) {

            return new VendorErrorResponse(
                    ResponseCodes.INVALID_SIGNATURE.getHttpStatus(),
                    List.of(ErrorResponse.of(ResponseCodes.INVALID_SIGNATURE)));
        }
        return new VendorErrorResponse(
                ResponseCodes.INVALID_SIGNATURE.getHttpStatus(),
                ErrorResponse.of(ResponseCodes.INVALID_SIGNATURE));
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    protected final void checkSecret(String expectedSignature, String signature) {
        if (!expectedSignature.equals(signature)) {
            throw new SignatureValidationException("Secret does not match");
        }
    }

    private String resolveField(String key, String rawBody, Map<String, String> formFields) {

        if (formFields != null) {
            String v = formFields.get(key);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();

            List<Map<String, Object>> list =
                    mapper.readValue(rawBody, new TypeReference<>() {
                    });

            if (list.isEmpty()) {
                return null;
            }

            Object v = list.get(0).get(key);
            return v == null ? null : String.valueOf(v);

        } catch (Exception e) {
            throw new SignatureValidationException("Invalid JSON body", e);
        }
    }

    private String requireField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new SignatureValidationException("Missing " + fieldName);
        }
        return value;
    }
}