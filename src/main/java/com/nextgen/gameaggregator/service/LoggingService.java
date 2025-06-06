package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.util.EnvUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class LoggingService {
    private static final ThreadLocal<AtomicInteger> logCounterHolder = ThreadLocal.withInitial(AtomicInteger::new);
    @Value("${logging.process-time:false}")
    private Boolean enableProcessTime;
    @Value("${logging.enable-vendor-data-flow-logs:false}")
    private boolean enableVendorDataFlowLogs;
    // Inject list of numbers
    @Value("${logging.data-flow-logs-vendor-list:}") // example value in properties or yml file > 1,4,19
    private String vendorList;
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
        if ((endTime - this.startTime) > 1000) {
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

    /**
     * Logs the data flow with a counter if logging is enabled.
     *
     * @param description Description of the current step in the data flow
     * @param data        The data being logged
     */
    public void logDataFlowByVendor(String description, Integer vendorId, String roundId, Object data) {
        if (!enableVendorDataFlowLogs) return;
        if (!EnvUtils.getVendorListFromEnv(this.vendorList).contains(vendorId)) return;

        AtomicInteger logCounter = logCounterHolder.get(); // Get the counter for the current thread

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("FunctionName", "logDataFlowByVendor -> " + description);
        jsonObject.put("Count", logCounter.incrementAndGet()); // Increment the log counter atomically
        jsonObject.put("RoundId", Objects.requireNonNullElse(roundId, JSONObject.NULL));
        jsonObject.put("Data", new Gson().toJson(data));
        jsonObject.put("Time", System.currentTimeMillis());
        log.info(jsonObject.toString());
    }

    public void logRequestDetails(HttpServletRequest request, StringBuilder requestBody, String traceId) {
        HashMap<String, Object> logInfo = new HashMap<>();

        // record basic request info
        logInfo.put("traceId", traceId);
        logInfo.put("requestUri", request.getRequestURI());
        logInfo.put("requestMethod", request.getMethod());
        logInfo.put("requestQueryString", request.getQueryString());
        logInfo.put("requestAddress", request.getRemoteAddr());
        logInfo.put("requestHost", request.getRemoteHost());
        logInfo.put("requestPort", request.getRemotePort());
        logInfo.put("protocol", request.getProtocol());
        logInfo.put("requestBody", String.valueOf(requestBody));
        log.info(new Gson().toJson(logInfo));
    }
}
