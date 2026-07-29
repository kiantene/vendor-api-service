package com.nextgen.gameaggregator.vendor.hp100.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hp100.config.HP100VendorConfig;
import com.nextgen.gameaggregator.vendor.hp100.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hp100.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.hp100.response.FailResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HP100SignatureValidator extends AbstractVendorSignatureValidator {

    protected HP100SignatureValidator(VendorPlayerDataService vendorPlayerDataService
            , VendorLineService vendorLineService, GameSessionDataService gameSessionDataService) {
        super(vendorPlayerDataService, vendorLineService, gameSessionDataService);
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        try {
            String vendorsecret = formFields.get("secret");
            String secret = getCredentialAccessorByKeyValue(Credentials.VENDOR_ID, "secret", formFields.get("secret")).getValue("secret");
            ValidationUtils.isEquals(secret, vendorsecret, SignatureValidationException::new);
        } catch (Exception e) {
            throw new SignatureValidationException("Signature validation failed", e);
        }
        return ValidationResult.success();
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        FailResponse response = new FailResponse();
        response.setData("");
        response.setErrCode(ResponseCode.INVALID_SECRET_KEY.code);
        response.setMessage(ResponseCode.INVALID_SECRET_KEY.message);
        return new VendorErrorResponse(ResponseCode.INVALID_SECRET_KEY.httpStatus, response);
    }

    @Override
    public String getVendorClassName() {
        return HP100VendorConfig.CLASS_NAME;
    }
}
