package com.nextgen.gameaggregator.data.kafka.betdetails;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BetDetailEmitRequest {
    String vendor;
    String eventFamily;
    EventKind eventKind;
    String vendorBetId;
    String gaBetId;
    String roundId;
    String vendorPlayerUsername;
    Integer agentId;
    String requestBody;
    /*
     * Non-null only for re-issued/amended result events (e.g. Pinnacle resettles, where the
     * original settle's idempotency key would otherwise collide). Appended as ":v{n}" to the
     * key so Stage-2 does not collapse the resettle into the original settle.
     * Rule is per-vendor and must be mirrored in C.1 so both producers compute the same key:
     *   Pinnacle SETTLED: pass action.Id when action.WagerInfo.resettlementTime is non-null.
     */
    Long resettleVersion;
}
