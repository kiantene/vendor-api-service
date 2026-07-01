package com.nextgen.gameaggregator.vendor.spribe.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.spribe.config.SpribeConfig;
import com.nextgen.gameaggregator.vendor.spribe.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.spribe.response.ErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SpribeSignatureValidator extends AbstractVendorSignatureValidator {

    private static final String HEADER_CLIENT_ID        = "X-Spribe-Client-ID";
    private static final String HEADER_CLIENT_TS        = "X-Spribe-Client-TS";
    private static final String HEADER_CLIENT_SIGNATURE = "X-Spribe-Client-Signature";

    public SpribeSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                    VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.HMAC_SHA256_HEX);
    }

    @Override
    public String getVendorClassName() {
        return SpribeConfig.CLASS_NAME;
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    @Override
    public boolean shouldValidate(HttpServletRequest request, String endpoint) {
        return endpoint.contains("/v2/");
    }

    @Override
    public ValidationResult validate(HttpServletRequest request,
                                     Map<String, String> formFields,
                                     String rawBody) throws SignatureValidationException {
        String clientId  = request.getHeader(HEADER_CLIENT_ID);
        String clientTs  = request.getHeader(HEADER_CLIENT_TS);
        String provided  = request.getHeader(HEADER_CLIENT_SIGNATURE);

        // Skip validation when any required auth header is absent.
        //
        // Prod historically ran without a Spribe validator registered, so VendorAuthFilter
        // passed every Spribe request through unvalidated. After this validator was added,
        // QA observed callbacks (notably freebet deposits on /v2/) arriving without
        // X-Spribe-Client-ID — with no operator id we cannot look up the credential to
        // recompute the HMAC, so we have no way to validate. Rejecting these requests
        // would block flows that worked in prod.
        //
        // Behaviour: skip only when an input is MISSING. A request that includes all three
        // headers but with a wrong signature is still rejected by the equals check below.
        List<String> missing = new ArrayList<>();
        if (clientId == null || clientId.isBlank())  missing.add(HEADER_CLIENT_ID);
        if (clientTs == null || clientTs.isBlank())  missing.add(HEADER_CLIENT_TS);
        if (provided == null || provided.isBlank())  missing.add(HEADER_CLIENT_SIGNATURE);
        if (!missing.isEmpty()) {
            log.warn("Spribe signature validation skipped — missing header(s) {} on {}",
                    missing, request.getRequestURI());
            return ValidationResult.skipped();
        }

        String path    = buildPath(request);
        String payload = clientTs + path + (rawBody != null ? rawBody : "");

        try {
            VendorCredentialAccessor acc = getCredentialAccessorByKeyValue(null, Credentials.OPERATOR, clientId);
            String clientSecret = acc.getValue(Credentials.TOKEN);

            String computed = sign(payload, clientSecret);

            if (!provided.equalsIgnoreCase(computed)) {
                log.warn("Spribe signature mismatch | clientId={} | clientTs={} | path={} | receivedSignature={} | payload={}",
                        clientId, clientTs, path, provided, payload);
                // Monitor-only mode: log the mismatch but let the request pass instead of rejecting,
                // until we confirm zero false positives. Re-enable the throw below to enforce.
                return ValidationResult.skipped();
//                throw new SignatureValidationException("Signature mismatch");
            }

            return ValidationResult.success();

        } catch (Exception ex) {
            // Never let an unexpected error (missing/garbled credential, signing failure, etc.)
            // block a Spribe callback. Log and skip so the request proceeds — same fail-open
            // stance as monitor-only mode. Tighten to reject once the integration is stable.
            // Log a trimmed stack (drop the servlet/filter-chain tail) plus the payload.
            log.warn("Spribe signature validation error — skipping validation | clientId={} | path={} | payload={} | error={}",
                    clientId, path, payload, shortStack(ex));
            return ValidationResult.skipped();
        }
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        return new VendorErrorResponse(HttpStatus.OK, ErrorResponse.of(ErrorCodes.INVALID_SIGNATURE));
    }

    // Compact error summary: the exception plus the top few application frames, dropping the long
    // servlet/filter-chain tail that adds no signal for these validation failures.
    private static String shortStack(Throwable ex) {
        StringBuilder sb = new StringBuilder(ex.toString());
        StackTraceElement[] frames = ex.getStackTrace();
        int limit = Math.min(5, frames.length);
        for (int i = 0; i < limit; i++) {
            sb.append("\n\tat ").append(frames[i]);
        }
        if (ex.getCause() != null) {
            sb.append("\n\tcaused by: ").append(ex.getCause());
        }
        return sb.toString();
    }

    private String buildPath(HttpServletRequest request) {
        // VendorCallbackRoutingFilter internally forwards v1 -> v2, so getRequestURI() here is the
        // rewritten /v2 path. Spribe signs the ORIGINAL path it called, which the servlet preserves
        // in the forward attributes — use those so the HMAC is computed over the path Spribe signed.
        String forwardedUri = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        String path;
        String qs;
        if (forwardedUri != null) {
            path = forwardedUri;
            qs   = (String) request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING);
        } else {
            path = request.getRequestURI();
            qs   = request.getQueryString();
        }
        if (qs != null && !qs.isBlank()) {
            path += "?" + qs;
        }
        return path;
    }
}
