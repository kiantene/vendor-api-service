package com.nextgen.gameaggregator.vendor.grpc.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorWalletBalanceServiceRequestDto {

    private String hash;
    private String providerId;
    private String userId;
    private String token;

    public VendorWalletBalanceServiceRequestDto() {
    }

    public VendorWalletBalanceServiceRequestDto(String hash, String providerId, String userId, String token) {
        this.hash = hash;
        this.providerId = providerId;
        this.userId = userId;
        this.token = token;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
