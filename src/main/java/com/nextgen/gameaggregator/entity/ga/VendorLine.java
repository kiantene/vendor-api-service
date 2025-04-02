package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vendor_lines")
@Data
public class VendorLine {
    @Id
    private Integer id;
    private String name;
    private Integer vendorId;
    private Integer houseId;
    private Integer status;
}
