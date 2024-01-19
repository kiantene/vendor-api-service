package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vendor_line_currencies")
@Data
public class VendorLineCurrency {

    @Id
    private Integer id;
    private Integer vendorLineId;
    private Integer currencyId;
    private String vendorCurrencyCode;
    private Integer status;
}
