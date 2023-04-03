package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "vendor_currencies")
@Data
public class VendorCurrency {

    @Id
    private Integer id;
    private Integer vendorId;
    private String vendorCurrencyCode;
    private Integer status;

    @ManyToOne
    private Currency currency;
}
