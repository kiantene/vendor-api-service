package com.nextgen.gameaggregator.entity;

import javax.persistence.*;
import lombok.Data;

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
    private Integer houseId;
    private Integer status;
    @OneToMany(mappedBy = "vendorLineId", fetch = FetchType.EAGER)
    private List<VendorLineCredential> credentials;
}
