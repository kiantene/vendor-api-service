package com.nextgen.gameaggregator.vendor.api.v1.game.loginaction.dto;

public class LoginDto {

    private Long agentPlayerId;

    private Long vendorCredentialId;

    private Long vendorId;

    private Long gameId;

    private String language;

    private String platform;

    private String currency;

    private Long agentId;

    private String playerUsername;

    private Long houseId;

    private Long masterAgentId;

    private String traceId;

    private Long walletType;

    public LoginDto(Long agentPlayerId, Long vendorCredentialId, Long vendorId, Long gameId, String language, String platform, String currency, Long agentId, String playerUsername, Long houseId, Long masterAgentId, String traceId, Long walletType) {
        this.agentPlayerId = agentPlayerId;
        this.vendorCredentialId = vendorCredentialId;
        this.vendorId = vendorId;
        this.gameId = gameId;
        this.language = language;
        this.platform = platform;
        this.currency = currency;
        this.agentId = agentId;
        this.playerUsername = playerUsername;
        this.houseId = houseId;
        this.masterAgentId = masterAgentId;
        this.traceId = traceId;
        this.walletType = walletType;
    }

    public Long getAgentPlayerId() {
        return agentPlayerId;
    }

    public void setAgentPlayerId(Long agentPlayerId) {
        this.agentPlayerId = agentPlayerId;
    }

    public Long getVendorCredentialId() {
        return vendorCredentialId;
    }

    public void setVendorCredentialId(Long vendorCredentialId) {
        this.vendorCredentialId = vendorCredentialId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    public void setPlayerUsername(String playerUsername) {
        this.playerUsername = playerUsername;
    }

    public Long getHouseId() {
        return houseId;
    }

    public void setHouseId(Long houseId) {
        this.houseId = houseId;
    }

    public Long getMasterAgentId() {
        return masterAgentId;
    }

    public void setMasterAgentId(Long masterAgentId) {
        this.masterAgentId = masterAgentId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getWalletType() {
        return walletType;
    }

    public void setWalletType(Long walletType) {
        this.walletType = walletType;
    }
}
