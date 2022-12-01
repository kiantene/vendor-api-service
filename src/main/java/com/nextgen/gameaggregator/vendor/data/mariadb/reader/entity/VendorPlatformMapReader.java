
package com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "vendor_platform_maps")
@SQLDelete(sql = "UPDATE vendor_platform_maps SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")

public class VendorPlatformMapReader extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vendorId;

    @Column(name = "platform_code", nullable = false)
    private String platformCode;

    @Column(name = "vendor_platform_code", nullable = false)
    private String vendorPlatformCode;

    @Column(name = "is_default", nullable = false)
    private Long isDefault;

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

    public Long getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Long isDefault) {
        this.isDefault = isDefault;
    }
}


