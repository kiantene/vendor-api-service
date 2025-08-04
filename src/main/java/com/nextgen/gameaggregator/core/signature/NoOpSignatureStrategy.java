package com.nextgen.gameaggregator.core.signature;

public class NoOpSignatureStrategy implements SignatureStrategy {
    @Override
    public String sign(String payload, String secret) {
        return "";
    }

    @Override
    public String sign(Object payload, String secret) {
        return "";
    }
}
