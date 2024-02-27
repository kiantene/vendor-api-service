package com.nextgen.gameaggregator.vendor.pinnacle.utils;

import lombok.Data;

@Data
public class Signature {
    private String signature;
    private String timestamp;
    private String aesKey;

    public Signature(String signature, String timestamp, String aesKey) {
        this.signature = signature;
        this.timestamp = timestamp;
        this.aesKey = aesKey;
    }
}
