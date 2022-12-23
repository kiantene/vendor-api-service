package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.endround;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EndRoundVo {

    private BigDecimal cash;
    private BigDecimal bonus;
    private Integer error;
    private String description;
}
