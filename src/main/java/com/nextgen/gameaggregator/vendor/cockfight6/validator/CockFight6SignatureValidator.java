package com.nextgen.gameaggregator.vendor.cockfight6.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.cockfight6.response.FailResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CockFight6SignatureValidator extends AbstractVendorSignatureValidator {
    protected CockFight6SignatureValidator(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService, GameSessionDataService gameSessionDataService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.HMAC_SHA256_BASE64);
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String username = formFields.get("external_player_id");
        String signature = request.getHeader("signature");
        long ts = Long.parseLong(request.getHeader("ts"));
        if (signature == null || signature.isEmpty() || ts == 0) {
            throw new SignatureValidationException("Signature is missing in the request header");
        }
        if (username == null || username.isEmpty()) {
            throw new SignatureValidationException("Invalid UserName", new PlayerNotFoundException());
        }
        String sn;
        String secretKey;
        try {
            sn = getCredentialValueByUsername(username, Credentials.SN);
            secretKey = getCredentialValueByUsername(username, Credentials.WALLET_KEY);
        } catch (Exception ex) {
            throw new SignatureValidationException("Invalid UserName", new PlayerNotFoundException());
        }

        String compactJsonBody = rawBody.replaceAll("\\s+", "");
        String payload = sn + "_" + ts + "_" + compactJsonBody;

        checkSignature(signature, payload, secretKey);


        return ValidationResult.success();
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }


    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception) {
        return new VendorErrorResponse(ResponseCode.SUCCESS.httpStatus,
                FailResponse.builder()
                        .code(ResponseCode.INVALID_PLAYER.code)
                        .message(ResponseCode.INVALID_PLAYER.message)
                        .build());
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        return new VendorErrorResponse(ResponseCode.SUCCESS.httpStatus,
                FailResponse.builder()
                        .code(ResponseCode.INVALID_SECRET_KEY.code)
                        .message(ResponseCode.INVALID_SECRET_KEY.message)
                        .build());
    }
}
