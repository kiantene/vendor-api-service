package com.nextgen.gameaggregator.vendor.grpc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorGameAuthenticationServiceRequestDto {
    private String hash;
    private String token;
    private String providerId;

    public VendorGameAuthenticationServiceRequestDto() {
    }

    public VendorGameAuthenticationServiceRequestDto(String hash, String token, String providerId) {
        this.hash = hash;
        this.token = token;
        this.providerId = providerId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
