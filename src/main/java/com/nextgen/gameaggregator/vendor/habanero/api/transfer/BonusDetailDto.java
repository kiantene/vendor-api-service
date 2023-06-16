package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BonusDetailDto {

    @JsonProperty("bonusbalanceid")
    public String bonusBalanceId;

    @JsonProperty("couponid")
    public String couponId;

    @JsonProperty("coupontypeid")
    public Integer couponTypeId;

    @JsonProperty("couponcode")
    public String couponCode;
}
