package com.nextgen.gameaggregator.entity;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
