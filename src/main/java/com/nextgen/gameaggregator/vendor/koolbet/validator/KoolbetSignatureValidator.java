package com.nextgen.gameaggregator.vendor.koolbet.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KoolbetSignatureValidator extends AbstractVendorSignatureValidator {

    private static final String VENDOR_PLAYER_USERNAME = "username";

    protected KoolbetSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
            VendorLineService vendorLineService, GameSessionDataService gameSessionDataService) {
        super(vendorPlayerDataService, vendorLineService, gameSessionDataService);
    }

    @Override
    public boolean shouldValidate(HttpServletRequest request, String endpoint) {
        boolean skip =
                    endpoint.endsWith(EndPoints.TOKEN) ||
                    endpoint.endsWith(EndPoints.BALANCE) ||
                    endpoint.endsWith(EndPoints.CANCEL_BET) ||
                    endpoint.endsWith(EndPoints.CANCEL_SESSION_BET);

        return !skip;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        // Retrieve the game session using the token from form fields
        GameSession gameSession;
        try{
            gameSession = getGameSessionByToken(formFields.get("token"));
        } catch (Exception e) {
            throw new SignatureValidationException("Invalid token");
        }

        // Always return success with vendorPlayerUsername since signature validation is skipped for Koolbet
        return ValidationResult.success(Map.of(
                VENDOR_PLAYER_USERNAME, gameSession.getVendorPlayerUsername()
        ));
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        CommonResponse response = new CommonResponse();
        response.setErrorCode(ResponseCode.TOKEN_EXPIRED.code);
        return new VendorErrorResponse(ResponseCode.TOKEN_EXPIRED.httpStatus, response);
    }
}
