package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PromoWinResponse {

    private Integer err;
    private String txid;
    private BigDecimal bln;
    private String pid;
}
