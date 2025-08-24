package com.nextgen.gameaggregator.core.filter;

import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureService;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureValidatorRegistry;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class VendorSignatureFilter extends OncePerRequestFilter {
    private final VendorSignatureValidatorRegistry registry;
    private final VendorSignatureService service;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Vendors vendor = Vendors.fromRequestURI(request.getRequestURI());

        if (vendor == null || !vendor.isNewFramework()) {
            filterChain.doFilter(request, response);
            return;
        }

        VendorSignatureValidator validator = registry.getValidator(vendor.getClassName());

        if (validator == null) {
            filterChain.doFilter(request, response);
            return;
        }

        ResettableRequestWrapper wrapped = (ResettableRequestWrapper) request;
        if (service.doValidation(validator, wrapped, response)) {
            filterChain.doFilter(wrapped, response);
        }
    }
}
