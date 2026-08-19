package com.nextgen.gameaggregator.vendor.mtlive.validator;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
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
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MtliveSignatureValidator extends AbstractVendorSignatureValidator {

    private static final String PARAM_USER_ID = "user_id";
    private static final String PARAM_MSG = "msg";

    private static final String ERR_INVALID_PARAM = "INVALID_PARAMETER";

    // Retained locally because vendorLineService in AbstractVendorSignatureValidator is private
    // and lacks a protected getter, preventing direct reuse in subclass methods.
    private final VendorLineService vendorLineService;

    protected MtliveSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                       VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.MD5_REVERSE);
        this.vendorLineService = vendorLineService;
    }

    @Override
    public String getVendorClassName() {
        return MtliveConfig.CLASS_NAME;
    }

    /**
     * Validates incoming MTLive request signatures, headers, and player existence.
     *
     * <p>The player is resolved first because the signing credentials come from the player's own
     * vendor line (so one line's mis-configuration cannot break players on other lines that share
     * a clientId). Crucially, any "player not found" verdict is <strong>deferred until after the
     * signature is verified</strong>: a caller who cannot produce a valid signature always gets the
     * same generic signature error, so player existence is never exposed as an enumeration oracle,
     * while a genuine (signature-valid) MTLive request still receives PLAYER_NOT_FOUND.
     */
    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        logRequest(request, formFields);

        String contentType = request.getContentType();
        if (contentType == null || !contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            throw new SignatureValidationException(ERR_INVALID_PARAM);
        }

        String username = formFields.get(PARAM_USER_ID);
        if (username == null || username.isBlank()) {
            throw new SignatureValidationException(ERR_INVALID_PARAM);
        }

        // Resolve the player, but DEFER any "not found" verdict until AFTER the signature check
        // so existence is never revealed to a caller that cannot produce a valid signature.
        Optional<VendorPlayer> playerOpt = resolvePlayer(username);
        Integer vendorLineId = playerOpt.map(VendorPlayer::getVendorLineId).orElse(null);

        // Verify the signature. A resolved player uses their own vendor line (fault isolation);
        // an unresolved player falls back to the clientId lookup solely to obtain a secret to
        // verify the signature (never applies to a real player, so no shared-line blast radius).
        validateHeadersAndSignature(request, formFields, vendorLineId);

        // Signature is valid from here on; it is now safe to surface player problems.
        if (playerOpt.isEmpty()) {
            // Routed to onPlayerNotFound() via the PlayerNotFoundException cause -> PLAYER_NOT_FOUND.
            throw new SignatureValidationException(ERR_INVALID_PARAM, new PlayerNotFoundException());
        }
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
        // TODO(GA-14820): TEMPORARY INFO logging of the decrypted MTLive payload for go-live monitoring.
        // Revert to log.debug once prod traffic is verified stable. This emits decrypted player data
        // (user_id, amounts, txn ids) on every request, so it MUST NOT remain at INFO long-term.
        String formattedFormFields = formFields.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("\n"));
        log.info("MTLive Request Body: \n{}\n\nRequest Header: \n{}\n\nRequest URI: \n{}",
                formattedFormFields, getHeaders(request), request.getRequestURI());
    }

    private void validateHeadersAndSignature(HttpServletRequest request, Map<String, String> formFields, Integer vendorLineId) {
        String signature = request.getHeader(Headers.API_SI);
        String clientId = request.getHeader(Headers.API_CI);
        String timestamp = request.getHeader(Headers.API_TS);
        if (signature == null || timestamp == null || clientId == null) {
            throw new SignatureValidationException("Missing required security headers");
        }

        VendorCredentialAccessor accessor;
        if (vendorLineId != null) {
            // Resolved player: use their own vendor line so one line's mis-configuration cannot
            // affect players on other lines that share the same clientId.
            accessor = getCredentialAccessorByVendorLineId(vendorLineId);

            // Safety verification to guarantee incoming X-API-CI matches credentials resolved for player's line
            String expectedClientId = accessor.getValue(Credentials.CLIENT_ID);
            if (!clientId.equals(expectedClientId)) {
                log.warn("Mismatched client ID header. Received: {}, Expected for line {}: {}", clientId, vendorLineId, expectedClientId);
                throw new SignatureValidationException("Header X-API-CI does not match player vendor line credentials");
            }
        } else {
            // Unresolved player: no line to key on. Resolve by clientId SOLELY to verify the signature.
            // This branch never applies to a resolvable player, so it does not reintroduce the
            // shared-line blast-radius risk that user_id resolution exists to avoid.
            try {
                accessor = getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.CLIENT_ID, clientId);
            } catch (InternalConfigurationException e) {
                // Unknown clientId (no MTLive line matches). Must NOT bubble as a raw 500: that would
                // make an unknown clientId distinguishable from a known-clientId-with-bad-signature
                // (a clientId-enumeration oracle). Fail as a generic signature error, indistinguishable
                // from a bad signature, with no PlayerNotFoundException cause so player existence is not leaked.
                log.warn("MTLive credential resolution failed for unknown clientId during signature validation");
                throw new SignatureValidationException("Signature does not match");
            }
        }

        // getValue() guarantees a non-null, non-blank String, or throws InternalConfigurationException
        String clientSecret = accessor.getValue(Credentials.CLIENT_SECRET);

        checkSignature(signature, formFields.get(PARAM_MSG), timestamp + clientSecret + clientId);
    }

    private Optional<VendorPlayer> resolvePlayer(String username) {
        try {
            // getVendorPlayerByUsername never returns null (it throws when absent), so Optional.of is safe.
            return Optional.of(getVendorPlayerByUsername(username));
        } catch (EntityNotFoundException e) {
            // Genuine "this player does not exist". Return empty and DEFER the PLAYER_NOT_FOUND
            // verdict to the caller until after the signature is verified, so existence is not
            // revealed to callers that cannot produce a valid signature.
            return Optional.empty();
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

        Optional<VendorCredentialAccessor> accessorOpt = resolveCredentialAccessor(formFields);
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

        Optional<VendorCredentialAccessor> accessorOpt = resolveCredentialAccessor(formFields);
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
            // X-API-SI is the request signature (credential material) with no monitoring value; redact it.
            String headerValue = Headers.API_SI.equalsIgnoreCase(headerName)
                    ? "***"
                    : request.getHeader(headerName);
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
     * Strategic Order: user_id -> Header X-API-CI.
     * Primary lookup resolves line credentials via the player's user_id.
     * Fallback lookup reads X-API-CI when user_id is missing or unmapped.
     */
    private Optional<VendorCredentialAccessor> resolveCredentialAccessor(Map<String, String> formFields) {
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

    /**
     * Resolves credentials via the request's {@code client_id} header scoped to MTLive.
     *
     * <p>Retrieves the candidate line ID and verifies it belongs to the expected MTLive vendor ID.
     * If the returned line belongs to another vendor or cannot be found, fails fast
     * with an internal configuration exception.
     */
    @Override
    protected VendorCredentialAccessor getCredentialAccessorByKeyValue(Integer vendorId, String keyName, String keyValue) {
        try {
            // 1. Fetch line ID from VendorLineService (returns first active match)
            Integer vendorLineId = vendorLineService.getVendorLineIdListByNameAndValue(keyName, keyValue);

            // 2. Load the VendorLine to verify existence and ownership
            VendorLine vendorLine = vendorLineService.getVendorLine(vendorLineId);
            if (vendorLine == null) {
                log.error("VendorLine with ID {} for {}={} not found in database",
                        vendorLineId, keyName, keyValue);
                throw new InternalConfigurationException(
                        String.format("Vendor line %d not found for credential %s=%s", vendorLineId, keyName, keyValue));
            }

            if (!Objects.equals(vendorLine.getVendorId(), vendorId)) {
                log.error("Resolved vendorLineId {} for {}={} belongs to vendorId {}, expected {}",
                        vendorLineId, keyName, keyValue, vendorLine.getVendorId(), vendorId);
                throw new InternalConfigurationException(
                        String.format("Credential %s=%s resolved to line %d belonging to vendorId %d, expected vendorId %d",
                                keyName, keyValue, vendorLineId, vendorLine.getVendorId(), vendorId));
            }

            return new VendorCredentialAccessor(vendorLineService.mapCredentialsByName(vendorLineId));
        } catch (CredentialNotFoundException ex) {
            throw new InternalConfigurationException(keyName + " not found", ex);
        }
    }
}
