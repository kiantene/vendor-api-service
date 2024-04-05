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
}