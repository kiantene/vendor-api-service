package com.nextgen.gameaggregator.vendor.booongo.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Override
    public Integer operatorTimeoutTiming() {
        return 2500;
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }
}
