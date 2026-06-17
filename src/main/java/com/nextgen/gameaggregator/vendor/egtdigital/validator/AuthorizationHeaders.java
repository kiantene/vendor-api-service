package com.nextgen.gameaggregator.vendor.egtdigital.validator;

import lombok.Getter;

@Getter
public class AuthorizationHeaders {
    private final String checksum;
    private final String checksumFields;

    public AuthorizationHeaders(String checksum, String checksumFields) {
        this.checksum = checksum;
        this.checksumFields = checksumFields;
    }
}