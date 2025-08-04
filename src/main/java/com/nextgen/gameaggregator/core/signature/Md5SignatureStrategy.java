package com.nextgen.gameaggregator.core.signature;

import org.apache.commons.codec.digest.DigestUtils;

public class Md5SignatureStrategy implements SignatureStrategy {
    @Override
    public String sign(String payload, String secret) {
        return DigestUtils.md5Hex(payload + secret);
    }
}
