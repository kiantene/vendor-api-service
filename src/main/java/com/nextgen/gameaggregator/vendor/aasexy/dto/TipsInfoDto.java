package com.nextgen.gameaggregator.vendor.aasexy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipsInfoDto {
    private String unitPrice;
    private String quantity;
    private String receiverId;
    private String giftName;
    private String tableId;
    private String dealerDomain;

}
