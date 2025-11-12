package com.nextgen.gameaggregator.vendor.facai.api.promo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class PromoPayoutList {
    @JsonProperty("List")
    private List<PromoPayout> list;
    @JsonProperty("Ts")
    private Long timestamp;
}
