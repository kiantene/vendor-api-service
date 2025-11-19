package com.nextgen.gameaggregator.vendor.jdb.api.promo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Builder
@Getter
public class PromoPayoutResponse {
    @JsonProperty("status")
    private String status;
    @JsonProperty("balance")
    private BigDecimal balance;
    @JsonProperty("err_text")
    private String errText;
}
