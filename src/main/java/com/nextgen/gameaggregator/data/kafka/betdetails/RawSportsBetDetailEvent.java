package com.nextgen.gameaggregator.data.kafka.betdetails;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RawSportsBetDetailEvent {

    private String idempotencyKey;
    private String vendor;
    private EventKind eventKind;
    private String vendorBetId;
    private String gaBetId;
    private String roundId;
    private String vendorPlayerUsername;
    private Integer agentId;
    private String producerSource;
    private long receivedAt;

    @JsonRawValue
    private String requestBody;
}
