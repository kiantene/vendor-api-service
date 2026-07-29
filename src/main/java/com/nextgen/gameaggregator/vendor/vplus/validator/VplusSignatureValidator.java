package com.nextgen.gameaggregator.vendor.vplus.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.vplus.config.VplusConfig;
import com.nextgen.gameaggregator.vendor.vplus.constant.Credentials;
import com.nextgen.gameaggregator.vendor.vplus.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.vplus.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.vplus.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VplusSignatureValidator extends AbstractVendorSignatureValidator {
    private static final String HEADER_SIGN = "X-Sign";

    protected VplusSignatureValidator(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService, GameSessionDataService gameSessionDataService) {
        super(vendorPlayerDataService, vendorLineService, gameSessionDataService, SigningStrategyType.MD5);
    }

    @Override
    public String getVendorClassName() {
        return VplusConfig.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String username = formFields.get("username");
        String token = formFields.get("token");

        // Token validation (skip for balance API)
        boolean isBalanceApi = request.getRequestURI().equals(EndPoints.PATH + EndPoints.BALANCE);

        if (!isBalanceApi) {

            if (token == null || token.isBlank()) {
                throw new SignatureValidationException("Missing token");
            }

            try{
                getGameSessionByVendorToken(token);
            } catch (Exception e) {
                throw new SignatureValidationException("Invalid token");
            }
        }

        //Validate vendor signature
        String signatureHeader = request.getHeader(HEADER_SIGN);
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new SignatureValidationException("Missing Authorization header");
        }

        String appSecret;
        try {
            appSecret = getCredentialValueByUsername(username, Credentials.APP_SECRET);
        } catch (Exception ex) {
            throw new SignatureValidationException("Invalid username: " + username);
        }

        if (appSecret == null || appSecret.isBlank()) {
            throw new SignatureValidationException("Missing Credentials appSecret");
        }
        checkSignature(signatureHeader, rawBody, appSecret);
        return ValidationResult.success();
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return new VendorErrorResponse(
                ResponseCodes.VERIFICATION_FAILED.getHttpStatus(),
                ErrorResponse.of(ResponseCodes.VERIFICATION_FAILED)
        );
    }
}
