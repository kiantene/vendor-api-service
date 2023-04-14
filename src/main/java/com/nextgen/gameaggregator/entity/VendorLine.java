package com.nextgen.gameaggregator.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "vendor_lines")
@Data
public class VendorLine {
    @Id
    private Integer id;
    private String name;
    @ManyToOne
    private Vendor vendor;
//    private Boolean isMultiCurrency;
//    @ManyToOne
//    private Currency currency;
//    private String vendorCurrencyCode;
    private Integer houseId;
    private Integer status;
    @OneToMany(mappedBy = "vendorLineId", fetch = FetchType.EAGER)
    private List<VendorLineCredential> credentials;
}
