package com.nextgen.gameaggregator.core.filter;

import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.common.RequestParserService;
import com.nextgen.gameaggregator.core.security.VendorSecurityAdapter;
import com.nextgen.gameaggregator.core.security.VendorSecurityRegistry;
import com.nextgen.gameaggregator.core.security.decrypter.VendorDecryptionService;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureService;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Vendors vendor = resolveVendor(request);
        if (vendor == null) { chain.doFilter(request, response); return; }

        VendorSecurityAdapter adapter = securityRegistry.get(vendor.getClassName());
        ResettableRequestWrapper wrapped =
                (request instanceof ResettableRequestWrapper r) ? r : new ResettableRequestWrapper(request);

        String originalBody = wrapped.getCachedBody();
        Map<String, String> parsedFields = parseBody(request, originalBody);

        if (!handleDecryption(adapter, wrapped, response, parsedFields)) return;
        if (!handleValidation(adapter, wrapped, response, parsedFields)) return;

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
