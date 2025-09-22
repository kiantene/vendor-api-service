package com.nextgen.gameaggregator.core.common;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

public class SignatureGenerator {
    private static final Gson GSON = new Gson();
    public static final HmacAlgorithms HMAC_ALGORITHM = HmacAlgorithms.HMAC_SHA_256;

    private SignatureGenerator() {}

    public static String generate(Object payload, String secret) {
        String json = GSON.toJson(payload);
        return new HmacUtils(HMAC_ALGORITHM, secret).hmacHex(json);
    }
}
