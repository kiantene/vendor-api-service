package com.nextgen.gameaggregator.vendorapiservice.vendorclass.seamless.pragmaticplay.dto;

public class GameLoginRequest {

    private String secureLogin;
    private String symbol;
    private String language;
    private String token;
    private String currency;
    private String platform;

    public GameLoginRequest() {
    }

    public GameLoginRequest(String secureLogin, String symbol, String language, String token, String currency, String platform) {
        this.secureLogin = secureLogin;
        this.symbol = symbol;
        this.language = language;
        this.token = token;
        this.currency = currency;
        this.platform = platform;
    }

    public String getSecureLogin() {
        return secureLogin;
    }

    public void setSecureLogin(String secureLogin) {
        this.secureLogin = secureLogin;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
