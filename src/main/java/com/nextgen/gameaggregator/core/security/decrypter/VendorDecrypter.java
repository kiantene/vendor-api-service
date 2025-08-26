package com.nextgen.gameaggregator.core.security.decrypter;

import com.nextgen.core.exception.DecryptionException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.vendor.VendorComponent;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VendorDecrypter extends VendorComponent {
    DecryptionResult doDecryption(HttpServletRequest request, Map<String, String> formFields, String rawBody);
    VendorErrorResponse onDecryptionFailure(HttpServletRequest request, DecryptionException e);
}
