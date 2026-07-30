package com.nextgen.gameaggregator.vendor.evoplay.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ActionName;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;

import com.google.gson.reflect.TypeToken;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EvoPlaySignatureValidator extends AbstractVendorSignatureValidator {
    private static final String FIELD_NAME = "name";
    private static final String ATTR_USERNAME = "username";
    private static final String FIELD_SIGNATURE = "signature";
    private static final String FIELD_TOKEN = "token";
    private static final String FIELD_PROJECT = "project";
    private static final String FIELD_VERSION = "version";
    private static final String ACTION_WIN = ActionName.win.name();
    private static final String ACTION_REFUND = ActionName.refund.name();
    private static final String ACTION_BALANCE_INCREASE = ActionName.balanceincrease.name();
    private static final String FORM_DATA_USER_ID = "data[user_id]";
    private static final String FORM_DATA_ACTION_ID = "data[action_id]";
    private static final String FORM_DATA_REFUND_ACTION_ID = "data[refund_action_id]";
    private static final String TX_ID_BET_PREFIX = "::BET::";

    private final GameTransactionService gameTransactionService;

    private record ValidationResultData(String username) {
    }

    protected EvoPlaySignatureValidator(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService, GameSessionDataService gameSessionDataService, GameTransactionService gameTransactionService) {
        super(vendorPlayerDataService, vendorLineService, gameSessionDataService, SigningStrategyType.MD5);
        this.gameTransactionService = gameTransactionService;
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) {
        // Parse rawBody into formFields if it's empty
        if (formFields.isEmpty() && rawBody != null && !rawBody.isEmpty()) {
            formFields = VendorService.convertBodyToDto(rawBody, new TypeToken<Map<String, String>>() {
            }.getType());
        }
        GameSession gameSession;
        String username;
        String name = formFields.get(FIELD_NAME);
        if (ACTION_BALANCE_INCREASE.equalsIgnoreCase(name)) {
            username = formFields.get(FORM_DATA_USER_ID);
            if (username == null || username.trim().isEmpty()) {
                throw new SignatureValidationException("Missing username");
            }
            ValidationResultData validationResultData = new ValidationResultData(username);
            return ValidationResult.success(Map.of(ATTR_USERNAME, validationResultData.username()));
        }
        String vendorSignature = formFields.get(FIELD_SIGNATURE);
        String token = formFields.get(FIELD_TOKEN);

        try {
            gameSession = getGameSessionByToken(token);
            username = gameSession.getVendorPlayerUsername();
        } catch (GameSessionExpiredException ex) {
            if (ACTION_WIN.equals(name) || ACTION_REFUND.equals(name)) {
                username = recoverUsernameFromTransaction(name, formFields);
            } else {
                throw ex;
            }
        }
        ValidationResultData validationResultData = new ValidationResultData(username);

        // Verify signature
        String projectId = getCredentialValueByUsername(username, Credentials.PROJ_ID);
        String key = getCredentialValueByUsername(username, Credentials.KEY);

        formFields.remove(FIELD_SIGNATURE);
        formFields.put(FIELD_PROJECT, projectId);
        formFields.put(FIELD_VERSION, Formats.CALLBACK_VERSION);
        EvoPlaySignatureValidator.rearrangeMapString(formFields);

        MultiValueMap<String, String> formData = VendorService.flattenMapIntoMultiValueMap(formFields, "");

        String signature = sign(VendorService.buildSignature(formData, key), "");

        ValidationUtils.isEquals(signature, vendorSignature, SignatureValidationException::new);

        return ValidationResult.success(Map.of(ATTR_USERNAME, validationResultData.username()));
    }

    private static void rearrangeMapString(Map<String, String> originalMap) {
        String[] specificKeys = {FIELD_PROJECT, FIELD_VERSION};
        Map<String, String> rearrangedMap = new LinkedHashMap<>();

        for (String key : specificKeys) {
            if (originalMap.containsKey(key)) {
                rearrangedMap.put(key, originalMap.get(key));
                originalMap.remove(key);
            }
        }
        rearrangedMap.putAll(originalMap);
        originalMap.clear();
        originalMap.putAll(rearrangedMap);
    }

    private String recoverUsernameFromTransaction(String action, Map<String, String> formFields) {
        String vendorBetId = ACTION_WIN.equals(action)
                ? formFields.get(FORM_DATA_ACTION_ID)
                : formFields.get(FORM_DATA_REFUND_ACTION_ID);

        if (vendorBetId == null) throw new SignatureValidationException("Missing action_id for recovery");

        String txId = EndPoints.CLASS_NAME + TX_ID_BET_PREFIX + vendorBetId;

        return gameTransactionService.get(txId)
                .map(GameTransaction::getUsername)
                .orElseThrow(() -> new SignatureValidationException("Session expired and no historical Bet found for ID: " + txId));
    }
}