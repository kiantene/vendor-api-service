package com.nextgen.gameaggregator.core.security;

import com.nextgen.gameaggregator.core.security.decrypter.VendorDecrypter;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureValidator;

import java.util.Optional;

public record DefaultVendorSecurityAdapter(
        Optional<VendorDecrypter> decrypter,
        Optional<VendorSignatureValidator> validator
) implements VendorSecurityAdapter {

    public DefaultVendorSecurityAdapter(VendorDecrypter dec,
                                        VendorSignatureValidator val) {
        this(Optional.ofNullable(dec), Optional.ofNullable(val));
    }

    public static DefaultVendorSecurityAdapter empty() {
        return new DefaultVendorSecurityAdapter(Optional.empty(), Optional.empty());
    }
}
