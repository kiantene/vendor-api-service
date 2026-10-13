package com.nextgen.gameaggregator.vendor.megafair.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.megafair.constant.*;
import com.nextgen.gameaggregator.vendor.megafair.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.megafair.util.VendorUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nextgen.gameaggregator.vendor.megafair.constant.MegafairFields.*;

@Component
public class MegafairSignatureValidator extends AbstractVendorSignatureValidator {

    private final VendorUtil vendorUtil;

    private static final DateTimeFormatter VENDOR_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT).withLocale(Locale.ENGLISH);

    private static final List<String> AUTHENTICATE_KEYS = List.of(
            TOKEN, GAME_SESSION_ID
    );

    private static final List<String> BET_RESULT_KEYS = List.of(
            USER_ID, BET, WIN, IS_ROUND_FINISHED, ROUND_ID, GAME_ID, GAME_SESSION_ID
    );

    private static final List<String> REFUND_BET_KEYS = List.of(
            USER_ID, CASINO_TRANSACTION_ID, GAME_SESSION_ID
    );

    protected MegafairSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                         VendorLineService vendorLineService,
                                         GameSessionDataService gameSessionDataService,
                                         VendorUtil vendorUtil) {
        super(vendorPlayerDataService, vendorLineService, gameSessionDataService, SigningStrategyType.MD5);
        this.vendorUtil = vendorUtil;
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        populateFields(request, formFields);
        String queryStr = request.getQueryString();

        // Capture raw query string for OpenSearch vendor auditing
        LogContext logContext = LogContextHolder.get();
        if (logContext != null) {
            logContext.setBody(queryStr);
        }

        // 1. Validate endpoint support FIRST before looking up credentials
        String requestURI = request.getRequestURI();
        String toSign = getStringToSign(request.getRequestURI(), formFields);

        // 2. Resolve vendor credentials
        String hash = formFields.get(HASH);
        if (hash == null || hash.isBlank()) {
            throw new SignatureValidationException("Hash is null or empty");
        }
        String rawVendorUsername = formFields.get(USER_ID);
        String vendorUsername = rawVendorUsername == null ? null : rawVendorUsername.trim();
        String passKey;

        if (requestURI.equals(EndPoints.PATH + EndPoints.AUTHENTICATE)) {
            String token = formFields.get(TOKEN);
            if (token == null || token.isBlank()) {
                throw new SignatureValidationException("Token is null or empty for authenticate endpoint");
            }
            try {
                passKey = getCredentialValueByToken(token, Credentials.SECRET);
            } catch (GameSessionExpiredException ex) {
                throw new SignatureValidationException("Invalid session context or expired token for signature calculation", ex);
            }
        } else {
            passKey = getCredentialValueByUsername(vendorUsername, Credentials.SECRET);
        }

        if (passKey == null || passKey.isBlank()) {
            throw new SignatureValidationException("Missing Credentials passKey");
        }

        // 3. Verify hash against string-to-sign
        checkSignature(hash, toSign, passKey);

        return ValidationResult.success();
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception, Map<String, String> formFields) {
        String userId = formFields.get(USER_ID);

        ResponseCodes responseCode = (userId == null || userId.isBlank())
                ? ResponseCodes.INVALID_PARAMETERS   // Code 610: userId is missing or blank
                : ResponseCodes.USER_NOT_FOUND;       // Code 310: userId provided but user does not exist

        ErrorResponse response = mapToErrorResponse(formFields);
        response.setResponse(ErrorResponse.ResponseData.builder()
                .code(String.valueOf(responseCode.getCode()))
                .result(Results.ERROR)
                .build());

        return vendorUtil.toXmlVendorResponse(responseCode.getHttpStatus(), response);
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception, Map<String, String> formFields) {
        String exceptionMessage = exception.getMessage();

        ResponseCodes responseCode = getResponseCode(exceptionMessage, exception.getCause());

        ErrorResponse response = mapToErrorResponse(formFields);
        response.setResponse(ErrorResponse.ResponseData.builder()
                .code(String.valueOf(responseCode.getCode()))
                .result(Results.ERROR)
                .build());

        return vendorUtil.toXmlVendorResponse(responseCode.getHttpStatus(), response);
    }

    @NotNull
    private static ResponseCodes getResponseCode(String exceptionMessage, Throwable cause) {
        if (cause instanceof GameSessionExpiredException ||
                (exceptionMessage != null && exceptionMessage.contains("expired token"))) {
            return ResponseCodes.INVALID_TOKEN; // Code 400: Invalid Token / Expired session
        }

        boolean isMissingHash = exceptionMessage != null && exceptionMessage.contains("Hash is null or empty");
        boolean isMissingToken = exceptionMessage != null && exceptionMessage.contains("Token is null or empty");

        return (isMissingHash || isMissingToken)
                ? ResponseCodes.INVALID_PARAMETERS  // Code 610: Missing mandatory hash or token
                : ResponseCodes.INVALID_HASH;       // Code 500: Hash was provided but signature calculation failed
    }

    private String getStringToSign(String requestURI, Map<String, String> formFields) throws SignatureValidationException {
        return switch (requestURI) {
            case EndPoints.PATH + EndPoints.AUTHENTICATE -> getParamValues(formFields, AUTHENTICATE_KEYS);
            case EndPoints.PATH + EndPoints.BETRESULT -> getParamValues(formFields, BET_RESULT_KEYS);
            case EndPoints.PATH + EndPoints.REFUND -> getParamValues(formFields, REFUND_BET_KEYS);
            default -> throw new SignatureValidationException("Unsupported endpoint for signature validation: " + requestURI);
        };
    }

    /**
     * Concatenates parameter values in the exact order specified by the Megafair API spec.
     * <p>
     * NOTE: Per Megafair Integration API Spec Section 4.2 ("Security"), parameters are concatenated
     * directly without any delimiters.
     * Do NOT modify to add a delimiter as it will break signature validation against Megafair.
     */
    private String getParamValues(Map<String, String> formFields, List<String> paramKeys) {
        return paramKeys.stream()
                .map(k -> Optional.ofNullable(formFields.get(k)).orElse(""))
                .collect(Collectors.joining());
    }

    private ErrorResponse mapToErrorResponse(Map<String, String> formFields) {
        ErrorResponse.RequestData requestData = ErrorResponse.RequestData.builder()
                .userId(formFields.get(USER_ID))
                .bet(formFields.get(BET))
                .win(formFields.get(WIN))
                .roundId(formFields.get(ROUND_ID))
                .gameId(formFields.get(GAME_ID))
                .isRoundFinished(formFields.get(IS_ROUND_FINISHED))
                .gameSessionId(formFields.get(GAME_SESSION_ID))
                .casinoTransactionId(formFields.get(CASINO_TRANSACTION_ID))
                .kind(formFields.get(KIND))
                .hash(formFields.get(HASH))
                .token(formFields.get(TOKEN))
                .build();

        return ErrorResponse.builder()
                .request(requestData)
                .time(VENDOR_DATE_FORMATTER.format(ZonedDateTime.now()))
                .build();
    }

    private void populateFields(HttpServletRequest request, Map<String, String> formFields) {
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                formFields.put(key, values[0]);
            }
        });
    }
}
