package com.nextgen.gameaggregator.vendor.groove.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.groove.constant.Credentials;
import com.nextgen.gameaggregator.vendor.groove.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.groove.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.groove.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import static com.nextgen.gameaggregator.vendor.groove.util.VendorUtil.extractTokenFromSessionId;
import static com.nextgen.gameaggregator.vendor.groove.util.VendorUtil.generateSignature;

@Component
@Slf4j
public class GrooveSignatureValidator extends AbstractVendorSignatureValidator {
    private static final String HEADER_SIGN = "X-Groove-Signature";
    private static final String ACCOUNT_ID = "accountid";
    private static final String GAME_SESSION_ID = "gamesessionid";
    private static final String BET_AMOUNT = "betamount";
    private static final String RESULT_AMOUNT = "result";
    private static final String GAME_STATUS = "gamestatus";
    private static final String API_VERSION = "apiversion";

    private static final String INVALID_OPERATION_PARAMETER = "Invalid Operation Parameter";
    private static final Set<String> VALID_STATUSES = Set.of("completed", "pending");
    private static final Set<String> AMOUNT_PARAMS = Set.of(BET_AMOUNT, RESULT_AMOUNT);

    @Value("${is-test-env:false}")
    private boolean isTestEnv;

    protected GrooveSignatureValidator(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService);
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
        if (isTestEnv) return ValidationResult.success();
        log.info("Groove Request Header: \n" + getHeaders(request));
        String gameSessionId = request.getParameter(GAME_SESSION_ID);
        validateGameSession(gameSessionId);
        validateAmounts(request);
        validateGameStatus(request.getParameter(GAME_STATUS));

        String signature = request.getHeader(HEADER_SIGN);
        String username = request.getParameter(ACCOUNT_ID);

        if (signature == null || signature.isBlank()) {
            throw new SignatureValidationException("Missing Authorization header");
        }

        String securityKey = null;
        securityKey = getCredentialValueByUsername(username, Credentials.SECURITY_KEY);


        if (securityKey == null || securityKey.isBlank()) {
            try {
                String token = extractTokenFromSessionId(gameSessionId);
                securityKey = getCredentialValueByToken(token, Credentials.SECURITY_KEY);
            } catch (Exception ex) {
                log.error("Token lookup also failed for session: {}", gameSessionId);
            }
        }

        if (securityKey == null || securityKey.isBlank()) {
            throw new SignatureValidationException("Missing Credentials appSecret");
        }

        try {
            String queryString = request.getQueryString();
            if (!generateSignature(queryString, securityKey).equalsIgnoreCase(signature)) {
                throw new SignatureValidationException("Signature does not match");
            }
        } catch (Exception e) {
            throw new SignatureValidationException("Signature Retrieval failed: " + e.getMessage());
        }

        return ValidationResult.success();
    }

    private void validateGameSession(String gameSessionId) throws SignatureValidationException {
        if (gameSessionId == null || !gameSessionId.contains("_")) {
            throw new SignatureValidationException(INVALID_OPERATION_PARAMETER);
        }
    }

    private void validateAmounts(HttpServletRequest request) throws SignatureValidationException {
        for (String param : AMOUNT_PARAMS) {
            String value = request.getParameter(param);
            if (value != null && !value.isBlank()) {
                checkNumericAmount(value);
            }
        }
    }

    private void checkNumericAmount(String value) throws SignatureValidationException {
        try {
            BigDecimal numericValue = new BigDecimal(value.trim());
            if (numericValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new SignatureValidationException(INVALID_OPERATION_PARAMETER);
            }
        } catch (NumberFormatException e) {
            log.info("Invalid Bet Amount/Settle Amount format: {}", value);
        }
    }

    private void validateGameStatus(String status) throws SignatureValidationException {
        if (status != null && !status.isBlank() && !VALID_STATUSES.contains(status)) {
            throw new SignatureValidationException(INVALID_OPERATION_PARAMETER);
        }
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception, Map<String, String> formFields) {
        String apiVersion = getApiVersionFromContext();
        ErrorResponse response = new ErrorResponse();
        if (INVALID_OPERATION_PARAMETER.equals(exception.getMessage())) {
            response.setError(ResponseCode.OPERATION_NOT_ALLOWED);
        } else {
            response.setError(ResponseCode.INVALID_SIGNATURE);
        }
        response.setApiversion(apiVersion);
        return new VendorErrorResponse(HttpStatus.OK, response);
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception, Map<String, String> formFields) {
        String apiVersion = getApiVersionFromContext();
        ErrorResponse response = new ErrorResponse();
        response.setError(ResponseCode.OPERATION_NOT_ALLOWED);
        response.setApiversion(apiVersion);
        return new VendorErrorResponse(HttpStatus.OK, response);
    }

    public String getHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        StringBuilder headersString = new StringBuilder();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headersString.append(headerName)
                    .append(":")
                    .append(headerValue)
                    .append("\n");
        }
        return headersString.toString();
    }

    private String getApiVersionFromContext() {
        try {
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttrs) {
                return servletAttrs.getRequest().getParameter(API_VERSION);
            }
        } catch (Exception e) {
            log.warn("Failed to extract apiversion from RequestContextHolder context", e);
        }
        //Groove Default api version
        return "1.0";
    }
}