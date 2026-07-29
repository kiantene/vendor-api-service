package com.nextgen.gameaggregator.vendor.digitain.validator;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.digitain.config.DigitainConfig;
import com.nextgen.gameaggregator.vendor.digitain.constant.Credentials;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.digitain.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DigitainSignatureValidator extends AbstractVendorSignatureValidator {

    private static final String HEADER_AUTHORIZATION = "SecretKey";

    protected DigitainSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                         VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService);
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String signatureHeader = extractAuthorizationHeader(request);
        String appSecret;
        try {
            appSecret = getCredentialAccessorByKeyValue(DigitainConfig.DIGITAIN_VENDOR_ID, Credentials.SECRET_KEY, signatureHeader).getValue(Credentials.SECRET_KEY);

        } catch (InternalConfigurationException ex) {
            throw new SignatureValidationException("Missing Authorization header");
        }

        checkSecret(signatureHeader, appSecret);


        return ValidationResult.success();
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        return new VendorErrorResponse(HttpStatus.OK, new ErrorResponse(ResponseCode.WRONG_SECRET_KEY.code));
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception) {
        return new VendorErrorResponse(HttpStatus.OK, new ErrorResponse(ResponseCode.WRONG_SECRET_KEY.code));
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    private String extractAuthorizationHeader(HttpServletRequest request) throws SignatureValidationException {
        String signature = request.getHeader(HEADER_AUTHORIZATION);
        if (signature == null || signature.isBlank()) {
            throw new SignatureValidationException("Missing Authorization header");
        }
        return signature;
    }

    protected final void checkSecret(String signatureHeader, String appSecret) {
        if (!signatureHeader.equals(appSecret)) {
            throw new SignatureValidationException("Secret does not match");
        }
    }
}
