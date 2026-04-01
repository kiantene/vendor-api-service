package com.nextgen.gameaggregator.vendor.digitain.api.bet;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class BetResponse {

    private Integer err;
    private String txid;
    private BigDecimal bln;
    private String pid;

}
