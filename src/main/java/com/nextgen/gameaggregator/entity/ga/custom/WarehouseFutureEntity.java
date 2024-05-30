package com.nextgen.gameaggregator.entity.ga.custom;

import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.entity.ga.GameCategory;
import lombok.Data;

@Data
public class WarehouseFutureEntity {

    private VendorGame vendorGame;
    private Currency currency;
    private Vendor vendor;
    private GameCategory gameCategory;
}
