package com.nextgen.gameaggregator.entity.ga;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import lombok.Data;

@Data
public class HttpResponseLog {
    private String id;
    private Integer agentId;
    private Integer vendorId;
    private String url;
    private String operatorEndPoints;
    private Integer status;
    private Long requestStart;
    private Long requestEnd;
    private Long requestTimeTaken;
    private Long operatorStart;
    private Long operatorEnd;
    private Long operatorTimeTaken;
    private Long betStart;
    private Long betEnd;
    private Long betTimeTaken;
    private String errorMessage;
    private ResponseCodes.Status operatorResponseStatus;

    public HttpResponseLog(HttpRequestLog httpRequestLog) {
        this.id = httpRequestLog.getId();
        this.agentId = httpRequestLog.getAgentId();
        this.vendorId = httpRequestLog.getVendorId();
        this.url = httpRequestLog.getUrl();
        this.operatorEndPoints = httpRequestLog.getOperatorEndPoints();
        this.status = httpRequestLog.getStatus();
        this.requestStart = httpRequestLog.getStartTime();
        this.requestEnd = httpRequestLog.getEndTime();
        this.requestTimeTaken = httpRequestLog.getTimeTaken();
        this.operatorStart = httpRequestLog.getOperatorStart();
        this.operatorEnd = httpRequestLog.getOperatorEnd();
        this.operatorTimeTaken = httpRequestLog.getOperatorTimeTaken();
        this.betStart = httpRequestLog.getBetStart();
        this.betEnd = httpRequestLog.getBetEnd();
        this.betTimeTaken = httpRequestLog.getBetTimeTaken();
        this.errorMessage = httpRequestLog.getErrorMessage();
        this.operatorResponseStatus = httpRequestLog.getOperatorResponseStatus();
    }
}
