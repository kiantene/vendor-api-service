package com.nextgen.gameaggregator.core.filter;

import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.security.decrypter.VendorDecrypter;
import com.nextgen.gameaggregator.core.security.decrypter.VendorDecrypterRegistry;
import com.nextgen.gameaggregator.core.security.decrypter.VendorDecryptionService;
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
public class VendorDecryptionFilter extends OncePerRequestFilter {
    private final VendorDecrypterRegistry registry;
    private final VendorDecryptionService service;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Vendors vendor = Vendors.fromRequestURI(request.getRequestURI());

        if (vendor == null || !vendor.isNewFramework()) {
            filterChain.doFilter(request, response);
            return;
        }

        VendorDecrypter decrypter = registry.get(vendor.getClassName());

        if (decrypter == null) {
            filterChain.doFilter(request, response);
            return;
        }

        ResettableRequestWrapper wrapped = (ResettableRequestWrapper) request;
        if (service.doDecryption(decrypter, wrapped, response)) {
            filterChain.doFilter(wrapped, response);
        }
    }
}
