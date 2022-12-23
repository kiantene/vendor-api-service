package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.result;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResultVo {

    private String transactionId;
    private String currency;
    private BigDecimal cash;
    private BigDecimal bonus;
    private Integer error;
    private String description;
}
