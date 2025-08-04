package com.nextgen.gameaggregator.core.signature;

public interface SignatureStrategy {
    String sign(String payload, String secret);
}
