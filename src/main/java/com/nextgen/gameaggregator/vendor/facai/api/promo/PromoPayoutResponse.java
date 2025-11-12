package com.nextgen.gameaggregator.vendor.facai.api.promo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PromoPayoutResponse {
    @JsonProperty("Result")
    private Integer result;
}
