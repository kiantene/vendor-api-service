package com.nextgen.gameaggregator.vendor.wazdan.validator;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.wazdan.config.WazdanConfig;
import com.nextgen.gameaggregator.vendor.wazdan.constant.Credentials;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class WazdanSignatureValidator extends AbstractVendorSignatureValidator {

    protected WazdanSignatureValidator(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService);
    }

    @Override
    public String getVendorClassName() {
        return WazdanConfig.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String authHeader = request.getHeader("authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new SignatureValidationException("Missing Authorization header");
        }

        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

        try {
            getCredentialAccessorByKeyValue(WazdanConfig.ID, Credentials.WALLET_AUTH, credentials);
        } catch (InternalConfigurationException ex) {
            throw new SignatureValidationException("Invalid Authorization header");
        }
        return ValidationResult.success();
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return new VendorErrorResponse(HttpStatus.UNAUTHORIZED, "");
    }
}
