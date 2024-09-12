package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Slf4j
public class LoggingService {
    @Value("${logging.process-time:false}")
    private Boolean enableProcessTime;

    private Long startTime;

    public void logStart() {
        this.startTime = System.currentTimeMillis();
    }

    public void logProcessTime(String functionName, String traceId) {

        //TODO ENABLE BASED ON enableProcessTime STATUS
        if (!enableProcessTime) return;

        Long endTime = System.currentTimeMillis();
        Gson gson = new Gson();
        HashMap<String, Object> logInfo = new HashMap<>();
        logInfo.put("FunctionName: ", functionName);
        logInfo.put("TraceId: ", traceId);
        logInfo.put("StartTime: ", this.startTime);
        logInfo.put("EndTime: ", endTime);
        logInfo.put("TotalProcessMs: ", endTime - this.startTime);
        if((endTime - this.startTime) >1000){
            log.info(gson.toJson(logInfo));
        }

    }

    public void logProcessTimeTempLog(String functionName, String vendorPlayerUsername, String roundId) {

        Long endTime = System.currentTimeMillis();
        Long totalProcessMs = endTime - this.startTime;

        if (totalProcessMs >= 1000) {
            Gson gson = new Gson();
            HashMap<String, Object> logInfo = new HashMap<>();
            logInfo.put("FunctionName: ", functionName);
            logInfo.put("VendorPlayerUsername: ", vendorPlayerUsername);
            logInfo.put("RoundId: ", roundId);
            logInfo.put("StartTime: ", this.startTime);
            logInfo.put("EndTime: ", endTime);
            logInfo.put("TotalProcessMs: ", endTime - this.startTime);
            log.info(gson.toJson(logInfo));

        } else {
            //if processing time less than 1sec, then no log
        }

    }
}
