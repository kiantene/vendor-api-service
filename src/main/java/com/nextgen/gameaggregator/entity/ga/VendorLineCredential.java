package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.*;
import lombok.Data;

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
