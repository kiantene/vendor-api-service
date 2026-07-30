package com.nextgen.gameaggregator.vendor.mtlive.validator;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.mtlive.config.MtliveConfig;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Headers;
import com.nextgen.gameaggregator.vendor.mtlive.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.mtlive.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.mtlive.util.VendorUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MtliveSignatureValidator extends AbstractVendorSignatureValidator {

    private static final String PARAM_USER_ID = "user_id";
    private static final String PARAM_MSG = "msg";

    private static final String ERR_INVALID_PARAM = "INVALID_PARAMETER";

    protected MtliveSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                       VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.MD5_REVERSE);
    }

    @Override
    public String getVendorClassName() {
        return MtliveConfig.CLASS_NAME;
    }

    /**
     * Validates incoming MTLive request signatures, headers, and player existence.
     */
    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        logRequest(request, formFields);

        String contentType = request.getContentType();
        if (contentType == null || !contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            throw new SignatureValidationException(ERR_INVALID_PARAM);
        }

        validateHeadersAndSignature(request, formFields);

        String username = formFields.get(PARAM_USER_ID);
        if (username == null || username.isBlank()) {
            throw new SignatureValidationException(ERR_INVALID_PARAM);
        }

        // Verify player exists in DB before letting request proceed.
        VendorPlayer player = verifyPlayerExists(username);

        Integer vendorLineId = player.getVendorLineId();
        if (vendorLineId == null) {
            // A resolved MTLive player must map to a vendor line. A null here is a backend
            // data-integrity anomaly, not a client error: fail closed with a retriable 500
            // rather than letting the request reach response-encryption and NPE on the
            // credential lookup (which would surface as a confusing generic 500 anyway).
            log.error("MTLive player '{}' has no vendorLineId; cannot resolve encryption credentials",
                    username);
            throw new InternalServerException("MTLive player has no associated vendor line");
        }

        // Carry the resolved vendorLineId as a server-side request attribute (not a form field)
        // so an attacker-supplied raw-body value can never shadow it; response encryption reads
        // it back trusted-by-construction.
        request.setAttribute(VendorUtil.RESOLVED_VENDOR_LINE_ATTR, vendorLineId);

        // Return the decrypted form fields so the controller can bind the payload: the raw
        // request body only carried the encrypted `msg`, so these fields exist only after
        // decryption and reach the controller solely via enrichRequestFields(additionalFields).
        return ValidationResult.success(formFields);
    }

    private void logRequest(HttpServletRequest request, Map<String, String> formFields) {
        String formattedFormFields = formFields.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("\n"));
        log.debug("MTLive Request Body: \n{}\n\nRequest Header: \n{}\n\nRequest URI: \n{}",
                formattedFormFields, getHeaders(request), request.getRequestURI());
    }

    private void validateHeadersAndSignature(HttpServletRequest request, Map<String, String> formFields) {
        String signature = request.getHeader(Headers.API_SI);
        String clientId = request.getHeader(Headers.API_CI);
        String timestamp = request.getHeader(Headers.API_TS);
        if (signature == null || timestamp == null || clientId == null) {
            throw new SignatureValidationException("Missing required security headers");
        }

        VendorCredentialAccessor accessor = getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.CLIENT_ID, clientId);
        String clientSecret = accessor.getValue(Credentials.CLIENT_SECRET);
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new SignatureValidationException("Missing Credentials clientSecret");
        }

        checkSignature(signature, formFields.get(PARAM_MSG), timestamp + clientSecret + clientId);
    }

    private VendorPlayer verifyPlayerExists(String username) {
        try {
            return getVendorPlayerByUsername(username);
        } catch (EntityNotFoundException e) {
            // Genuine "this player does not exist" -> client error.
            // Routed to onPlayerNotFound() via the PlayerNotFoundException cause -> PLAYER_NOT_FOUND.
            throw new SignatureValidationException(ERR_INVALID_PARAM, new PlayerNotFoundException());
        } catch (RuntimeException e) {
            // Infrastructure fault (DB/cache down, timeout, pool exhaustion). This is NOT a bad
            // request. Reporting it as INVALID_PARAMETER tells MTLive the payload is permanently
            // wrong, so they will not retry -> a settle/rollback can be silently dropped.
            // Surface a retriable server error (500) instead so the vendor retries.
            log.error("MTLive player lookup failed due to a backend error; returning retriable server error", e);
            throw new InternalServerException("Player lookup failed during MTLive signature validation", e);
        }
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception, Map<String, String> formFields) {
        ErrorResponse response = new ErrorResponse(ResponseCode.DECRYPTION_ERROR);
        if (exception != null && exception.getMessage() != null && exception.getMessage().contains(ERR_INVALID_PARAM)) {
            response = new ErrorResponse(ResponseCode.INVALID_PARAMETER);
        }
        response.setTimestamp(Instant.now().getEpochSecond());

        Optional<VendorCredentialAccessor> accessorOpt = resolveCredentialAccessorForError(formFields);
        if (accessorOpt.isEmpty()) {
            log.warn("Cannot encrypt onInvalidSignature response: No fallback credentials found");
            return new VendorErrorResponse(HttpStatus.BAD_REQUEST, "Invalid signature or missing credentials");
        }

        String rawEncryptedMsg = VendorUtil.encryptResponse(response, accessorOpt.get()).getBody();
        return new VendorErrorResponse(HttpStatus.OK, rawEncryptedMsg);
    }

    @Override
    public VendorErrorResponse onPlayerNotFound(SignatureValidationException exception, Map<String, String> formFields) {
        ErrorResponse response = new ErrorResponse(ResponseCode.PLAYER_NOT_FOUND);
        response.setTimestamp(Instant.now().getEpochSecond());

        Optional<VendorCredentialAccessor> accessorOpt = resolveCredentialAccessorForError(formFields);
        if (accessorOpt.isEmpty()) {
            log.warn("Cannot encrypt onPlayerNotFound response: No fallback credentials found");
            return new VendorErrorResponse(HttpStatus.BAD_REQUEST, "Player not found and credentials unavailable");
        }

        String rawEncryptedMsg = VendorUtil.encryptResponse(response, accessorOpt.get()).getBody();
        return new VendorErrorResponse(HttpStatus.OK, rawEncryptedMsg);
    }

    @Override
    public boolean useNewEvents() {
        return true;
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

    /**
     * Tries multiple resolution strategies to ensure error responses can be encrypted
     * according to MTLive protocol expectations.
     * Strategic Order: user_id -> Header X-API-CI. The X-API-CI header is mandatory on every
     * well-formed request and is what signature validation already keys on, so it covers every
     * legitimate case; there is no further fallback.
     */
    private Optional<VendorCredentialAccessor> resolveCredentialAccessorForError(Map<String, String> formFields) {
        String username = formFields != null ? formFields.get(PARAM_USER_ID) : null;

        return resolveByUserId(username)
                .or(this::resolveByHeaderClientId);
    }

    private Optional<VendorCredentialAccessor> resolveByHeaderClientId() {
        try {
            String clientId = getRequestHeader();
            if (clientId != null && !clientId.isBlank()) {
                return Optional.of(getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.CLIENT_ID, clientId));
            }
        } catch (Exception e) {
            log.debug("Failed to resolve error credentials via X-API-CI header: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<VendorCredentialAccessor> resolveByUserId(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        try {
            VendorPlayer player = getVendorPlayerByUsername(username);
            if (player != null && player.getVendorLineId() != null) {
                return Optional.of(getCredentialAccessorByVendorLineId(player.getVendorLineId()));
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve credential accessor by user_id: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private String getRequestHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader(Headers.API_CI);
        }
        return null;
    }

    @Override
    public VendorCredentialAccessor getCredentialAccessorByKeyValue(Integer vendorId, String keyName, String keyValue) {
        return super.getCredentialAccessorByKeyValue(vendorId, keyName, keyValue);
    }
}