package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
