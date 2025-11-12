package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "vendor_currencies")
@Data
public class VendorCurrency {

    @Id
    private Integer id;
    private Integer vendorId;
    private String vendorCurrencyCode;
    private Integer status;
    private BigDecimal fromVendorRate;
    private BigDecimal toVendorRate;
    private Integer currencyId;
}
