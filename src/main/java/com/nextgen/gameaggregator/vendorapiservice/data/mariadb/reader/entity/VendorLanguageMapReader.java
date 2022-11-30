package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vendor_language_maps")
@SQLDelete(sql = "UPDATE vendor_language_maps SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")

public class VendorLanguageMapReader extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vendorId;

    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode;

    @Column(name = "vendor_language_code", nullable = false, length = 25)
    private String vendorLanguageCode;

    @Column(name = "is_default_language", nullable = false)
    private Boolean isDefaultLanguage;

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
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

    public Boolean getDefaultLanguage() {
        return isDefaultLanguage;
    }

    public void setDefaultLanguage(Boolean defaultLanguage) {
        isDefaultLanguage = defaultLanguage;
    }

    @Override
    public Long getId() {
        return null;
    }
}

