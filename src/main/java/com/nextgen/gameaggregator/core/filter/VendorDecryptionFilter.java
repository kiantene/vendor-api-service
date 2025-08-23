package com.nextgen.gameaggregator.core.filter;

import com.nextgen.core.exception.DecryptionException;
import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.common.RequestParserService;
import com.nextgen.gameaggregator.core.decrypter.VendorDecrypter;
import com.nextgen.gameaggregator.core.decrypter.VendorDecrypterRegistry;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.util.ResponseUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class VendorDecryptionFilter extends OncePerRequestFilter {
    private final VendorDecrypterRegistry registry;
    private final RequestParserService parserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String vendorClassName = SupportedVendors.extractVendorClassName(request.getRequestURI());

        if (vendorClassName.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        VendorDecrypter decrypter = registry.get(vendorClassName);

        if (decrypter == null) {
            filterChain.doFilter(request, response);
            return;
        }

        ResettableRequestWrapper wrapped = (ResettableRequestWrapper) request;
        if (doDecryption(decrypter, wrapped, response)) {
            filterChain.doFilter(wrapped, response);
        }
    }

    private boolean doDecryption(VendorDecrypter decrypter,
                                 ResettableRequestWrapper request,
                                 HttpServletResponse response) throws IOException {

        try {
            String rawBody = request.getCachedBody();
            Map<String, String> parsedFields = parserService.parse(request.getContentType(), rawBody);
            decrypter.doDecryption(request, parsedFields, rawBody);
            return true;
        } catch (DecryptionException ex) {
            LogContextHolder.get().setException(ex);
            VendorErrorResponse errorResponse = decrypter.onDecryptionFailure(request, ex);
            if (errorResponse == null || errorResponse.getBody() == null) {
                errorResponse = createDefaultDecryptionErrorResponse();
            }

            ResponseUtil.writeErrorResponse(response, errorResponse.getBody(), errorResponse.getStatusCode().value());
            return false;
        }
    }

    private VendorErrorResponse createDefaultDecryptionErrorResponse() {
        return new VendorErrorResponse(
                HttpStatus.UNAUTHORIZED,
                Map.of("error", "Decryption failed")
        );
    }
}
