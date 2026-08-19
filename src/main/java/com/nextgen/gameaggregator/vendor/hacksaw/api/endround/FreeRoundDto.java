package com.nextgen.gameaggregator.vendor.hacksaw.api.endround;

import lombok.Data;

@Data
public class FreeRoundDto {
    private Long freeRoundActivationId;

    private Integer sourceType;

    private Long internalId;

    private String externalId;

    private String campaignId;

    private String offerId;

    private Integer freeRoundsRemaining;
}
