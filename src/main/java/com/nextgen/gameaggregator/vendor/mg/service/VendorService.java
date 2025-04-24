package com.nextgen.gameaggregator.vendor.mg.service;


import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Getter
@Setter
public class VendorService extends BaseVendorService {
    private List<UnsettledBet> vendorClassFileUnsettledBetList;

    @Override
    public Integer operatorTimeoutTiming() {
        //mg vendor timeout is 4000, and given 500 buffer timing, then defaultTiming would be 3500.
        Integer operatorDefaultTiming = 3500;
        return operatorDefaultTiming;
    }
}