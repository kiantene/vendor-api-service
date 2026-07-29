package com.nextgen.gameaggregator.vendor.topbet.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.topbet.config.TopbetConfig;
import com.nextgen.gameaggregator.vendor.topbet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.topbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.topbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.topbet.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TopbetSignatureValidator extends AbstractVendorSignatureValidator {

    protected TopbetSignatureValidator(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.MD5);
    }

    @Override
    public boolean shouldValidate(HttpServletRequest request, String endpoint) {
        return !endpoint.equals(EndPoints.PATH + EndPoints.HEALTH);
    }

    @Override
    public String getVendorClassName() {
        return TopbetConfig.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String signature = formFields.get("sign");
        String merchantId = formFields.get("pid");
        String apiKey;
        // Verify signature
        try {
            apiKey = getCredentialAccessorByKeyValue(TopbetConfig.ID, Credentials.MERCHANT_ID, merchantId).getValue(Credentials.API_KEY);
        } catch (Exception ex) {
            throw new SignatureValidationException("Missing Credentials pid");
        }

        ValidationUtils.isEquals(VendorUtil.getSignature(new HashMap<>(formFields), apiKey), signature, SignatureValidationException::new);

        return ValidationResult.success();
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return new VendorErrorResponse(ResponseCode.SIGNATURE_VERIFICATION_FAILED.httpStatus, new ErrorResponse(ResponseCode.SIGNATURE_VERIFICATION_FAILED));
    }
}
