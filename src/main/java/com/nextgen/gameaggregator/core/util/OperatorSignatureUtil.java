package com.nextgen.gameaggregator.core.util;

import com.nextgen.core.security.signature.SignatureStrategy;
import com.nextgen.core.security.signature.SigningStrategyType;

public class OperatorSignatureUtil {
    private OperatorSignatureUtil() {

    }

    public static String sign(Object payload, String secret) {
        SignatureStrategy strategy = SigningStrategyType.HMAC_SHA256_HEX.getStrategy();
        return strategy.sign(payload, secret);
    }
}
