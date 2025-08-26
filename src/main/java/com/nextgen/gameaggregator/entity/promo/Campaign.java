package com.nextgen.gameaggregator.entity.promo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Campaign {
    private Integer id;
    private String uuid;
    private Integer campaignType;
    private long startTime;
    private long endTime;
    private String currencyCode;
    private Integer gameCount;
    private Byte status;
    private String vendorCampaignCode;
    private String vendorCampaignName;
    private Integer vendorId;
    private Integer vendorLineId;
    private String vendorCurrencyCode;
    private Integer playerCount;
    private Integer promoEngineStatus;
}
