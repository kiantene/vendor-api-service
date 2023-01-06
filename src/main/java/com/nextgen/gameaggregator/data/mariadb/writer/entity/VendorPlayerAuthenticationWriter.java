package com.nextgen.gameaggregator.data.mariadb.writer.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "vendor_player_authentications")
@SQLDelete(sql = "UPDATE vendor_player_authentications SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")

public class VendorPlayerAuthenticationWriter extends CommonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "wallet_type", nullable = false)
    private Long walletType;

    @Column(name = "agent_player_id", nullable = false)
    private Long agentPlayerId;

    @Column(name = "vendor_player_id", nullable = false)
    private Long vendorPlayerId;

    @Column(name = "vendor_player_username")
    private String vendorPlayerUsername;

    @Column(name = "platform_code", nullable = false,  length = 50)
    private String platformCode;

    @Column(name = "vendor_platform_code")
    private String vendorPlatformCode;

    @Column(name = "language_code", nullable = false,  length = 50)
    private String languageCode;

    @Column(name = "vendor_language_code")
    private String vendorLanguageCode;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "vendor_game_code")
    private String vendorGameCode;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "currency_code", nullable = false,  length = 50)
    private String currencyCode;

    @Column(name = "vendor_currency_code")
    private String vendorCurrencyCode;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Long getWalletType() {
        return walletType;
    }

    public void setWalletType(Long walletType) {
        this.walletType = walletType;
    }

    public Long getAgentPlayerId() {
        return agentPlayerId;
    }

    public void setAgentPlayerId(Long agentPlayerId) {
        this.agentPlayerId = agentPlayerId;
    }

    public Long getVendorPlayerId() {
        return vendorPlayerId;
    }

    public void setVendorPlayerId(Long vendorPlayerId) {
        this.vendorPlayerId = vendorPlayerId;
    }

    public String getVendorPlayerUsername() {
        return vendorPlayerUsername;
    }

    public void setVendorPlayerUsername(String vendorPlayerUsername) {
        this.vendorPlayerUsername = vendorPlayerUsername;
    }

    public String getPlatformCode() {
        return platformCode;
    }

    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode;
    }

    public String getVendorPlatformCode() {
        return vendorPlatformCode;
    }

    public void setVendorPlatformCode(String vendorPlatformCode) {
        this.vendorPlatformCode = vendorPlatformCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getVendorLanguageCode() {
        return vendorLanguageCode;
    }

    public void setVendorLanguageCode(String vendorLanguageCode) {
        this.vendorLanguageCode = vendorLanguageCode;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getVendorGameCode() {
        return vendorGameCode;
    }

    public void setVendorGameCode(String vendorGameCode) {
        this.vendorGameCode = vendorGameCode;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getVendorCurrencyCode() {
        return vendorCurrencyCode;
    }

    public void setVendorCurrencyCode(String vendorCurrencyCode) {
        this.vendorCurrencyCode = vendorCurrencyCode;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
