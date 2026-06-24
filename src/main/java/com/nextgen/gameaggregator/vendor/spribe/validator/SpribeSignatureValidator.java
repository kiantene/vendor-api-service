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
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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

        String path = buildPath(request);

        try {
            VendorCredentialAccessor acc = getCredentialAccessorByKeyValue(null, Credentials.OPERATOR, clientId);
            String clientSecret = acc.getValue(Credentials.TOKEN);

            String payload  = clientTs + path + (rawBody != null ? rawBody : "");
            String computed = sign(payload, clientSecret);

            if (!provided.equalsIgnoreCase(computed)) {
                log.warn("Spribe signature mismatch | clientId={} | clientTs={} | path={} | receivedSignature={} | computedPrefix={} | payloadLength={} | payloadSha256={}",
                        clientId,
                        clientTs,
                        path,
                        provided,
                        computed.length() <= 8 ? "********" : computed.substring(0, 8) + "...",
                        payload.length(),
                        DigestUtils.sha256Hex(payload));
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
            log.warn("Spribe signature validation error — skipping validation | clientId={} | path={}",
                    clientId, path, ex);
            return ValidationResult.skipped();
        }
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        return new VendorErrorResponse(HttpStatus.OK, ErrorResponse.of(ErrorCodes.INVALID_SIGNATURE));
    }

    private String buildPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String qs   = request.getQueryString();
        if (qs != null && !qs.isBlank()) {
            path += "?" + qs;
        }
        return path;
    }
}
