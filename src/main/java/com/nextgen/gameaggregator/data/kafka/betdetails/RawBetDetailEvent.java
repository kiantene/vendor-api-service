package com.nextgen.gameaggregator.data.kafka.betdetails;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RawBetDetailEvent {

    private String idempotencyKey;
    private String vendor;
    private EventKind eventKind;
    private String vendorBetId;
    private String gaBetId;
    private String roundId;
    private String vendorPlayerUsername;
    private Integer agentId;
    private Integer gameCategoryId;
    private String producerSource;
    private long receivedAt;
    private String bodyFormat;
    // Plain string — no @JsonRawValue. Livecasino vendors mix JSON ("json") and
    // URL-encoded querystring ("form") bodies; the downstream consumer parses
    // by bodyFormat. See plan v5 §5.
    private String requestBody;
}
