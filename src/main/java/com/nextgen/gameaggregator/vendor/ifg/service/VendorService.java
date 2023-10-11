package com.nextgen.gameaggregator.vendor.ifg.service;

import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorLineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Autowired
    private VendorLineService vendorLineService;
    public static Long getTimeStamp(String datetimeString) {
        try {
            // Use Instant.parse to directly convert the datetime string to an Instant
            Instant instant = Instant.parse(datetimeString);

            // Get the Unix timestamp in seconds
            return instant.toEpochMilli();
        } catch (Exception e) {
//            e.printStackTrace();
//            return -1L; // Return -1 for parsing error

            // Get the current system timestamp
            Instant currentTimestamp = Instant.now();

            // Convert the Instant to a Unix timestamp in seconds
            return currentTimestamp.toEpochMilli();
        }
    }

    public ResultType checkResult(String winAmount, String status){
        ResultType resultType = null;

        if(status.equals("0")){
            if(!winAmount.equals("0")){
                resultType = ResultType.WIN;
            }else{
                resultType = ResultType.LOSE;
            }
        }else{
            // status 1 means it is endround notice
            if(!winAmount.equals("0")){
                resultType = ResultType.WIN;
            }else{
                resultType = ResultType.END;
            }
        }

        return resultType;
    }

    public void verifyTokenStatus(Integer status) throws AuthenticationException {
        if (status != Status.ACTIVE.code) {
            throw new AuthenticationException();
        }
    }
}
