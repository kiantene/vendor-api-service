package com.nextgen.gameaggregator.vendor.esoterica.validator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.esoterica.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import com.nextgen.gameaggregator.vendor.esoterica.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.esoterica.util.VendorUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EsotericaSignatureValidator extends AbstractVendorSignatureValidator {

    protected EsotericaSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                          VendorLineService vendorLineService,
                                          GameSessionDataService gameSessionDataService) {
        super(vendorPlayerDataService, vendorLineService, gameSessionDataService, SigningStrategyType.SHA256_HEX);
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {

        log.debug("Esoterica request: fields={} rawBodyLen={}", redact(formFields), rawBody != null ? rawBody.length() : 0);

        String token = formFields.get(StringConstants.TOKEN);
        String userId = formFields.get(StringConstants.USER_ID);
        String hash = formFields.get(StringConstants.HASH);
        String betHash = formFields.get(StringConstants.BET_HASH);
        String betUserId = formFields.get(StringConstants.BET_USER_ID);
        String winHash = formFields.get(StringConstants.WIN_HASH);
        String winUserId = formFields.get(StringConstants.WIN_USER_ID);
        Integer vendorLineId;

        Map<String, Object> params = OBJECT_MAPPER.convertValue(formFields, new TypeReference<>() {});

        if (((hash == null || hash.isBlank()) && (betHash == null || betHash.isBlank()) && (winHash == null || winHash.isBlank())) ||
                ((token == null || token.isBlank()) && (userId == null || userId.isBlank()) && (betUserId == null || betUserId.isBlank()))) {
            throw new SignatureValidationException(StringConstants.INVALID_PARAMETER);
        }

        if (token != null && !token.isBlank() && token.matches("\\S+")) {
            vendorLineId = getVendorLineIdByToken(token);
        }
        else {
            if (userId != null && !userId.isBlank() && userId.matches("\\S+")) {
                vendorLineId = getVendorLineIdByUsername(userId);
            }
            else if (betUserId != null && !betUserId.isBlank() &&
                    winUserId != null && !winUserId.isBlank() &&
                    betUserId.matches("\\S+") &&
                    winUserId.matches("\\S+") &&
                    betHash != null && !betHash.isBlank() &&
                    winHash != null && !winHash.isBlank()) {

                if (!betUserId.equals(winUserId)) {
                    throw new SignatureValidationException(StringConstants.INVALID_PLAYER);
                }

                // handle bet.userId, bet.hash inside bet body
                vendorLineId = getVendorLineIdByUsername(betUserId);
            }
            else  {
                throw new SignatureValidationException(StringConstants.INVALID_PLAYER);
            }
        }

        String secretKey = getSecretKey(vendorLineId);

        if (request.getRequestURI().equals(EndPoints.PATH + EndPoints.BETANDRESULT)) {

            if (betHash == null || betHash.isBlank() || winHash == null || winHash.isBlank()) {
                throw new SignatureValidationException(StringConstants.INVALID_PARAMETER);
            }

            validateHash(params, secretKey, StringConstants.BET_PREFIX, betHash);
            validateHash(params, secretKey, StringConstants.WIN_PREFIX, winHash);
        }
        else {
            validateHash(params, secretKey, null, hash);
        }

        return ValidationResult.success();
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {

        CommonResponse commonResponse = new CommonResponse();
        commonResponse.setCash(BigDecimal.ZERO);
        commonResponse.setBonus(BigDecimal.ZERO);

        if (exception.getMessage().equals(StringConstants.INVALID_PARAMETER)) {
            commonResponse.setError(ResponseCodes.INVALID_PARAMETER.getCode());
            commonResponse.setDescription(ResponseCodes.INVALID_PARAMETER.getMessage());
        }
        else if (exception.getMessage().equals(StringConstants.INVALID_PLAYER)) {
            commonResponse.setError(ResponseCodes.PLAYER_NOT_FOUND.getCode());
            commonResponse.setDescription(ResponseCodes.PLAYER_NOT_FOUND.getMessage());
        }
        else {
            commonResponse.setError(ResponseCodes.TOKEN_NOT_FOUND.getCode());
            commonResponse.setDescription(ResponseCodes.TOKEN_NOT_FOUND.getMessage());
        }

        return new VendorErrorResponse(commonResponse);
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.PLAYER_NOT_FOUND));
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    private String getSecretKey(Integer vendorLineId) {
        VendorCredentialAccessor credentialsMap = getCredentialAccessorByVendorLineId(vendorLineId);
        if (credentialsMap == null) {
            throw new SignatureValidationException(StringConstants.INVALID_PLAYER);
        }
        return credentialsMap.getValue(StringConstants.SECRET_KEY);
    }

    private Integer getVendorLineIdByUsername(String username) {
        try {
            VendorPlayer vendorPlayer = getVendorPlayerByUsername(username);
            return vendorPlayer.getVendorLineId();
        } catch (Exception ex) {
            throw new SignatureValidationException(StringConstants.INVALID_PLAYER);
        }
    }

    private Integer getVendorLineIdByToken(String token) {
        try {
            GameSession gameSession = getGameSessionByToken(token);
            return gameSession.getVendorLineId();
        } catch (Exception ex) {
            throw new SignatureValidationException(StringConstants.INVALID_PLAYER);
        }
    }

    private void validateHash(Map<String, Object> params, String secretKey, String prefix, String hash) {
        String concatenatedString = VendorUtil.prepareRequest(params, secretKey, prefix);

        String hashedString = sign(concatenatedString, "").toUpperCase();

        if (!hashedString.equals(hash)) {
            throw new SignatureValidationException(StringConstants.INVALID_HASH);
        }
    }

    private Map<String, String> redact(Map<String, String> formFields) {
        if (formFields == null) {
            return Collections.emptyMap();
        }

        Map<String, String> redacted = new HashMap<>(formFields);
        redacted.remove(StringConstants.TOKEN);
        redacted.remove(StringConstants.HASH);
        redacted.remove(StringConstants.BET_HASH);
        redacted.remove(StringConstants.WIN_HASH);
        return redacted;
    }
}
