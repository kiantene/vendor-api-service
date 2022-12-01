package com.nextgen.gameaggregator.vendor.grpc.vo;

public class VendorGameAuthenticationServiceResponseVo {
    private String userId;
    private String currency;
    private Double cash;
    private Double bonus;
    private String token;
    private Integer error;
    private String description;

    public VendorGameAuthenticationServiceResponseVo() {
    }

    public VendorGameAuthenticationServiceResponseVo(String userId, String currency, Double cash, Double bonus, String token, Integer error, String description) {
        this.userId = userId;
        this.currency = currency;
        this.cash = cash;
        this.bonus = bonus;
        this.token = token;
        this.error = error;
        this.description = description;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getCash() {
        return cash;
    }

    public void setCash(Double cash) {
        this.cash = cash;
    }

    public Double getBonus() {
        return bonus;
    }

    public void setBonus(Double bonus) {
        this.bonus = bonus;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getError() {
        return error;
    }

    public void setError(Integer error) {
        this.error = error;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
