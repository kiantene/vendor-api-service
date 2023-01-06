package com.nextgen.gameaggregator.data.mariadb.reader.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "vendors")
@SQLDelete(sql = "UPDATE vendors SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")

public class VendorReader extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "vendor_code", nullable = false)
    private String vendorCode;

    @Column(name = "class_file", nullable = false)
    private String classFile;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getClassFile() {
        return classFile;
    }

    public void setClassFile(String classFile) {
        this.classFile = classFile;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
