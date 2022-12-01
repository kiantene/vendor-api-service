package com.nextgen.gameaggregator.vendor.api.v1.apicomponent.log;

import com.nextgen.gameaggregator.vendor.data.couchbase.entity.traceidlogrequest.TraceIdLogRepository;
import com.nextgen.gameaggregator.vendor.data.couchbase.entity.traceidlogrequest.TraceIdLogRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TraceIdLog {

    @Autowired
    private TraceIdLogRepository traceIdLogRepository;
    public void storeRequest(String traceId, String agentId, String action, String rawRequest){

        TraceIdLogRequest traceIdLogRequest =  new TraceIdLogRequest(agentId+"-"+traceId, agentId, Instant.now().toEpochMilli(), action, rawRequest);
        traceIdLogRepository.save(traceIdLogRequest);
    }
}
