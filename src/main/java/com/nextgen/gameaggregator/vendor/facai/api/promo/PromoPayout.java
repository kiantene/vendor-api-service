package com.nextgen.gameaggregator.vendor.facai.api.promo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class PromoPayout {
    private String eventID;
    private String memberAccount;
    private Long gameID;
    private String bankID;
    private String trsID;
    private BigDecimal points;
    private String createTime;
    private String eventType;
}
