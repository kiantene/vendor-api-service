package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "product_vendor_lines")
public class ProductVendorLine {
    @Id
    private Integer id;
    private Integer productId;
    private Integer vendorId;
    private Integer vendorLineId;
    private Integer gameCategoryId;
    private Integer currencyId;
    private Integer priority;
    private Integer status;
}
