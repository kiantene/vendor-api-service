package com.nextgen.gameaggregator.data.mariadb.reader.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "vendor_game_language_maps")
@SQLDelete(sql = "UPDATE vendor_game_language_maps SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")

public class VendorGameLanguageMapReader extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vendorGameId;

    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "vendor_open_game_code", nullable = false, length = 50)
    private String vendorOpenGameCode;

    @Column(name = "vendor_bet_game_code", nullable = false, length = 50)
    private String vendorBetGameCode;

    @Column(name = "platform_code", nullable = false, length = 50)
    private String platformCode;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;


    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Override
    public Long getId() {
        return null;
    }

    public Long getVendorGameId() {
        return vendorGameId;
    }

    public void setVendorGameId(Long vendorGameId) {
        this.vendorGameId = vendorGameId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendorOpenGameCode() {
        return vendorOpenGameCode;
    }

    public void setVendorOpenGameCode(String vendorOpenGameCode) {
        this.vendorOpenGameCode = vendorOpenGameCode;
    }

    public String getVendorBetGameCode() {
        return vendorBetGameCode;
    }

    public void setVendorBetGameCode(String vendorBetGameCode) {
        this.vendorBetGameCode = vendorBetGameCode;
    }

    public String getPlatformCode() {
        return platformCode;
    }

    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }
}

