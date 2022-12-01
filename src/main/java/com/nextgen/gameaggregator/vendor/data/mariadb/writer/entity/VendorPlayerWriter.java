package com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "vendor_players")
@SQLDelete(sql = "UPDATE vendor_players SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")

public class VendorPlayerWriter extends CommonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_player_id", nullable = false)
    private Long agentPlayerId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "vendor_credential_id", nullable = false)
    private Long vendorCredentialId;

    @Column(name = "credentials_version", nullable = false)
    private Long credentialsVersion;

    @Column(name = "vendor_username", nullable = false,  length = 50)
    private String vendorUsername;

    @Column(name = "currency_code", nullable = false,  length = 50)
    private String currencyCode;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentPlayerId() {
        return agentPlayerId;
    }

    public void setAgentPlayerId(Long agentPlayerId) {
        this.agentPlayerId = agentPlayerId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Long getVendorCredentialId() {
        return vendorCredentialId;
    }

    public void setVendorCredentialId(Long vendorCredentialId) {
        this.vendorCredentialId = vendorCredentialId;
    }

    public Long getCredentialsVersion() {
        return credentialsVersion;
    }

    public void setCredentialsVersion(Long credentialsVersion) {
        this.credentialsVersion = credentialsVersion;
    }

    public String getVendorUsername() {
        return vendorUsername;
    }

    public void setVendorUsername(String vendorUsername) {
        this.vendorUsername = vendorUsername;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
