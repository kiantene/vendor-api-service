package com.nextgen.gameaggregator.vendor.endorphina.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.game.launcher.endorphina.util.VendorUtil;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.endorphina.constant.Credentials;
import com.nextgen.gameaggregator.vendor.endorphina.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.endorphina.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EndorphinaSignatureValidator extends AbstractVendorSignatureValidator {

    protected EndorphinaSignatureValidator(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService
            , GameSessionDataService gameSessionDataService) {
        super(vendorPlayerDataService, vendorLineService, gameSessionDataService, SigningStrategyType.SHA1_HEX);
    }

    @Override
    public String getVendorClassName() {
        return Vendors.ENDORPHINA.getClassName();
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String expectedSignature = request.getParameter("sign");
        if (expectedSignature == null || expectedSignature.isBlank()) {
            throw new SignatureValidationException("Missing signature");
        }
        String vendorPlayerUsername = this.checkPlayer(request);
        String salt = getCredentialValueByUsername(vendorPlayerUsername, Credentials.SALT);

        if (salt == null || salt.isBlank()) {
            throw new SignatureValidationException("Missing credential SALT");
        }
        Map<String, String> combinedParams = VendorUtil.getMergeRequestParams(request, formFields);
        String signature = VendorUtil.getSignature(combinedParams, salt);
        String encryptSignature = sign(signature, "");
        checkSecret(expectedSignature, encryptSignature);
        return ValidationResult.success();
    }

    protected final void checkSecret(String expectedSignature, String signature) {
        if (!expectedSignature.equals(signature)) {
            throw new SignatureValidationException("Secret does not match");
        }
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.ACCESS_DENIED));
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.TOKEN_NOT_FOUND));
    }

    private String checkPlayer(HttpServletRequest request) {
        String player = request.getParameter("player");
        String token = request.getParameter("token");

        if (player != null && !player.isBlank()) {
            return player;
        } else if (token != null && !token.isBlank()) {
            try {
                GameSession gameSession = getGameSessionByToken(token);
                return gameSession.getVendorPlayerUsername();
            } catch (GameSessionExpiredException ex) {
                throw new SignatureValidationException("GameSession not found", new PlayerNotFoundException());
            }
        } else {
            throw new SignatureValidationException("Missing player", new PlayerNotFoundException());
        }
    }
}