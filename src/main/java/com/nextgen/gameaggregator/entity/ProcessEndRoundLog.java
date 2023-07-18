package com.nextgen.gameaggregator.entity;

import jakarta.persistence.Id;
import lombok.Data;

@Data
public class ProcessEndRoundLog {
    @Id
    private String traceId;
    private String roundId;
    private String vendorBetId;
    private String rawBody;
    private Long startTime;
    private Long endTime;
    private Long timeTaken;
    private Long operatorProcessStartTime;
    private Long operatorProcessEndTime;
    private Long operatorProcessTimeTaken;
    private Integer status;
}
