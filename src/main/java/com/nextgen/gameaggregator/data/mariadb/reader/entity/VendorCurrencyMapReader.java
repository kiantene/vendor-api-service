package com.nextgen.gameaggregator.data.mariadb.reader.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "vendor_currency_maps")
@SQLDelete(sql = "UPDATE vendor_currency_maps SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")

public class VendorCurrencyMapReader extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "currency_code", nullable = false, length = 5)
    private String currencyCode;

    @Column(name = "vendor_currency_code", nullable = false, length = 25)
    private String vendorCurrencyCode;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Override
    public Long getId() {
        return null;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
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
