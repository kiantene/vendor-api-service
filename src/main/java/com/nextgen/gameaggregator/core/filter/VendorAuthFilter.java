package com.nextgen.gameaggregator.core.filter;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.common.RequestParserService;
import com.nextgen.gameaggregator.core.exception.mapper.*;
import com.nextgen.gameaggregator.core.security.VendorSecurityAdapter;
import com.nextgen.gameaggregator.core.security.VendorSecurityRegistry;
import com.nextgen.gameaggregator.core.security.decrypter.VendorDecryptionService;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureService;
import com.nextgen.gameaggregator.core.util.ResponseUtil;
import com.nextgen.gameaggregator.vendor.Vendors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VendorAuthFilter extends OncePerRequestFilter {
    private final RequestParserService parserService;
    private final VendorSecurityRegistry securityRegistry;
    private final VendorDecryptionService decryptionService;
    private final VendorSignatureService signatureService;
    private final VendorExceptionMapperRegistry exceptionRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Vendors vendor = resolveVendor(request);
        if (vendor == null) { chain.doFilter(request, response); return; }

        String vendorClassName = vendor.getClassName();
        VendorSecurityAdapter adapter = securityRegistry.get(vendorClassName);
        ResettableRequestWrapper wrapped =
                (request instanceof ResettableRequestWrapper r) ? r : new ResettableRequestWrapper(request);

        String originalBody = wrapped.getCachedBody();

        try {
            Map<String, String> parsedFields = parseBody(request, originalBody);

            if (!handleDecryption(adapter, wrapped, response, parsedFields)) return;
            if (!handleValidation(adapter, wrapped, response, parsedFields)) return;
        } catch (InvalidRequestException ex) {
            VendorExceptionMapper mapper = exceptionRegistry.getMapper(vendorClassName);
            if (mapper != null) {
                VendorErrorResponse err = mapper.onInvalidRequestError(ex);
                ResponseUtil.writeErrorResponse(response, err.getBody(), err.getStatusCode().value());
            } else {
                log.warn("No exception mapper registered for vendor: " + vendorClassName);
            }
            return;
        }

        chain.doFilter(wrapped, response);
    }

    private Vendors resolveVendor(HttpServletRequest request) {
        Vendors v = Vendors.fromRequestURI(request.getRequestURI());
        return (v == null || !v.isNewFramework()) ? null : v;
    }

    private Map<String, String> parseBody(HttpServletRequest request, String body) {
        // MUTABLE & deterministic order
        return new LinkedHashMap<>(parserService.parse(request.getContentType(), body));
    }

    /** @return true to continue, false to stop (response already written) */
    private boolean handleDecryption(VendorSecurityAdapter adapter,
                                     ResettableRequestWrapper wrapped,
                                     HttpServletResponse response,
                                     Map<String, String> parsedFields) throws IOException {
        if (adapter.decrypter().isEmpty()) return true;

        var result = decryptionService.doDecryption(adapter.decrypter().get(), wrapped, response, parsedFields);
        if (!result.success()) return false;

        var injected = result.injectedFields();
        if (injected != null && !injected.isEmpty()) {
            parsedFields.putAll(injected);
        }
        String decrypted = result.decryptedText();
        if (decrypted != null && !decrypted.isBlank()) {
            parsedFields.put(VendorDecryptionService.KEY_DECRYPTED, decrypted);
        }
        return true;
    }

    /** @return true to continue, false to stop (response already written) */
    private boolean handleValidation(VendorSecurityAdapter adapter,
                                     ResettableRequestWrapper wrapped,
                                     HttpServletResponse response,
                                     Map<String, String> parsedFields) throws IOException {
        if (adapter.validator().isEmpty()) return true;

        var validator = adapter.validator().get();
        var outcome = signatureService.doValidation(validator, wrapped, response, parsedFields);

        if (outcome.isSkipped()) {
            // skip means: just proceed down the chain
            return true;
        }
        // invalid means response already written
        return outcome.valid();
    }
}
