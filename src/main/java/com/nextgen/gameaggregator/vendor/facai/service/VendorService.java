package com.nextgen.gameaggregator.vendor.facai.service;

import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.SettledBetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    SettledBetService settledBetService;

    public boolean isValidDateString(String timestamp, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        try {
            Date date = dateFormat.parse(timestamp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public SettledBet couchBaseCheckSettledRecord(Long vendorPlayerId, String externalBetId) {
        SettledBet checkRecord = null;

        try {
            checkRecord = settledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalBetId);
        } catch (BetNotFoundException e) {
            return null; //if record not found then return null;
        }

        return checkRecord;
    }

    @Override
    public Integer operatorTimeoutTiming() {
        //fc vendor timeout is 4000, and given 500 buffer timing, then defaultTiming would be 3500.
        Integer operatorDefaultTiming = 3500;
        return operatorDefaultTiming;
    }
}
