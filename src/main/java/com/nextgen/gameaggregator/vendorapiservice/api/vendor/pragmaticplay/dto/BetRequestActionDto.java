package com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.dto;

import java.math.BigDecimal;

public class BetRequestActionDto {
    private String hash;
    private String userId;
    private String gameId;
    private Long roundId;
    private BigDecimal amount;
    private String reference;
    private String providerId;
    private Long timestamp;
    private String roundDetails;
    private String bonusCode;
    private String platform;
    private String language;
    private BigDecimal jackpotContribution;
    private Long jackpotId;
    private String token;
    private String ipAddress;

    public BetRequestActionDto() {
    }
    public BetRequestActionDto(String hash, String userId, String gameId, Long roundId, BigDecimal amount, String reference, String providerId, Long timestamp, String roundDetails, String bonusCode, String platform, String language, BigDecimal jackpotContribution, Long jackpotId, String token, String ipAddress) {
        this.hash = hash;
        this.userId = userId;
        this.gameId = gameId;
        this.roundId = roundId;
        this.amount = amount;
        this.reference = reference;
        this.providerId = providerId;
        this.timestamp = timestamp;
        this.roundDetails = roundDetails;
        this.bonusCode = bonusCode;
        this.platform = platform;
        this.language = language;
        this.jackpotContribution = jackpotContribution;
        this.jackpotId = jackpotId;
        this.token = token;
        this.ipAddress = ipAddress;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRoundDetails() {
        return roundDetails;
    }

    public void setRoundDetails(String roundDetails) {
        this.roundDetails = roundDetails;
    }

    public String getBonusCode() {
        return bonusCode;
    }

    public void setBonusCode(String bonusCode) {
        this.bonusCode = bonusCode;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public BigDecimal getJackpotContribution() {
        return jackpotContribution;
    }

    public void setJackpotContribution(BigDecimal jackpotContribution) {
        this.jackpotContribution = jackpotContribution;
    }

    public Long getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(Long jackpotId) {
        this.jackpotId = jackpotId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
