package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class EvoplayFreeRoundPayoutRequest {
    String id;
    String userId;
    String eventId;
    String currency;
    BigDecimal amount;
    boolean playerUuidCampaignLookup;
}
