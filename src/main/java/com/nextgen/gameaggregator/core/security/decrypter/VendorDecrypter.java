package com.nextgen.gameaggregator.core.security.decrypter;

import com.nextgen.core.exception.DecryptionException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VendorDecrypter {
    String getVendorClassName();
    Map<String, String> doDecryption(HttpServletRequest request, Map<String, String> formFields, String rawBody);
    VendorErrorResponse onDecryptionFailure(HttpServletRequest request, DecryptionException e);
}
