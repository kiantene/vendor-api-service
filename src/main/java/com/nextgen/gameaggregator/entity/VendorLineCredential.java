package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vendor_line_credentials")
@Data
public class VendorLineCredential {
    @Id
    private Integer id;
    private Integer vendorLineId;
    private Integer version;
    private String name;
    private String value;
    private Integer status;
}
