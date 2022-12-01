package com.nextgen.gameaggregator.vendor.data.couchbase.entity.traceidlogrequest;

import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("log")
@Collection("traceid_request")
public class TraceIdLogRequest {

    @Id
    private String id;

    private String agentId;

    private Long receivedTime;

    private String action;

    private String rawRequest;

    public TraceIdLogRequest(String id, String agentId, Long receivedTime, String action, String rawRequest) {
        this.id = id;
        this.agentId = agentId;
        this.receivedTime = receivedTime;
        this.action = action;
        this.rawRequest = rawRequest;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public Long getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(Long receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRawRequest() {
        return rawRequest;
    }

    public void setRawRequest(String rawRequest) {
        this.rawRequest = rawRequest;
    }
}
