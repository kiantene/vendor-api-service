package com.nextgen.gameaggregator.entity.ga.custom;

import com.nextgen.gameaggregator.entity.ga.*;

import lombok.Data;

@Data
public class WarehouseFutureEntity {

    private VendorGame vendorGame;
    private Currency currency;
    private Vendor vendor;
    private GameCategory gameCategory;
    private Agent agent;
}
