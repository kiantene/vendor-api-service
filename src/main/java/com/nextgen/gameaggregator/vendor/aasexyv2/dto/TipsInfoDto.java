package com.nextgen.gameaggregator.vendor.aasexyv2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipsInfoDto {
    private String unitPrice;
    private String quantity;
    private String receiverId;
    private String giftName;
    @NotBlank
    private String tableId;
    private String dealerDomain;

}
