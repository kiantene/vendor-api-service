package com.nextgen.gameaggregator.vendor.hacksaw.api.bet;

import lombok.Data;

@Data
public class FreeRoundDto {
    private Long freeRoundActivationId;

    private String campaignId;

    private String offerId;

    private Integer freeRoundsRemaining;

    private Long freespinValue;
}
