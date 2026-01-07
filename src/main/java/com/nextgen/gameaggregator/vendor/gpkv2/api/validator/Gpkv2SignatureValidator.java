package com.nextgen.gameaggregator.vendor.gpkv2.api.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkv2.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Gpkv2SignatureValidator extends AbstractVendorSignatureValidator {
    private static final String HEADER_AUTHORIZATION = "X-GPK-SIGNATURE";

    protected Gpkv2SignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                      VendorLineService vendorLineService, GameSessionDataService gameSessionDataService) {
        super(vendorPlayerDataService, vendorLineService, gameSessionDataService, SigningStrategyType.HMAC_SHA256_HEX);
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String signatureHeader = extractAuthorizationHeader(request);
        String username = formFields.get("operator_player_id");
        if (username == null || username.isBlank()) {
            throw new SignatureValidationException("Missing operator_player_id");
        }
        String signKey = getCredentialValueByUsername(username, Credentials.SIGN_KEY);
        checkSignature(signatureHeader, rawBody, signKey);
        return ValidationResult.success();
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }
    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        CommonVo response = new CommonVo();
        response.setErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
        return new VendorErrorResponse(HttpStatus.OK, response);
    }

    private String extractAuthorizationHeader(HttpServletRequest request) throws SignatureValidationException {
        String signature = request.getHeader(HEADER_AUTHORIZATION);
        if (signature == null || signature.isBlank()) {
            throw new SignatureValidationException("Missing Authorization header");
        }
        return signature;
    }
}