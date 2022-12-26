package com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet;

import com.nextgen.gameaggregator.entity.VendorRawBetData;
import com.nextgen.gameaggregator.event.EventHandler;
import com.nextgen.gameaggregator.repository.VendorRawBetDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BetEventHandler implements EventHandler {

    @Autowired
    private VendorRawBetDataRepository vendorRawBetDataRepository;

    public void on(String event, String data) {
        log.info(data);
        VendorRawBetData entity = new VendorRawBetData();

        entity.setVendorId(1);
        entity.setData(data);
        entity.setCreateDate(System.currentTimeMillis());

        vendorRawBetDataRepository.save(entity);
    }
}
