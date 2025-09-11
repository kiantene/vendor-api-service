package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "vendor_features")
@Data
public class VendorFeature {
    @Id
    private String id;
    private Integer featureId;
    private Integer vendorId;
    private Integer status;
}
