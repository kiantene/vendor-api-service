package com.nextgen.gameaggregator.core.security;

import com.nextgen.gameaggregator.core.security.decrypter.VendorDecrypter;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureValidator;

import java.util.Optional;

public interface VendorSecurityAdapter {
    Optional<VendorDecrypter> decrypter();
    Optional<VendorSignatureValidator> validator();
}
