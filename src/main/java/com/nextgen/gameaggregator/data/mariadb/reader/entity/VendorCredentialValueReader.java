package com.nextgen.gameaggregator.data.mariadb.reader.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "vendor_credential_values")
@SQLDelete(sql = "UPDATE vendor_credential_values SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")

public class VendorCredentialValueReader extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_credential_id", nullable = false)
    private Long vendorCredentialId;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendorCredentialId() {
        return vendorCredentialId;
    }

    public void setVendorCredentialId(Long vendorCredentialId) {
        this.vendorCredentialId = vendorCredentialId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
