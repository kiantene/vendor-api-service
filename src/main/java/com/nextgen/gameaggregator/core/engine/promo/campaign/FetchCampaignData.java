package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchCampaignData {
    private String uuid;
    private Integer status;
    private Long completedAt;
}
