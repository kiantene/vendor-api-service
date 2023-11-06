package com.nextgen.gameaggregator.vendor.pinnacle.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultVo {
    private Boolean Available;
    private String UserCode;
    private BigDecimal AvailableBalance;
}
