package com.nextgen.gameaggregator.vendor.pinnacle.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultVo {
    @JsonProperty("Available")
    private Boolean Available;

    @JsonProperty("UserCode")
    private String userCode;

    @JsonProperty("AvailableBalance")
    private BigDecimal availableBalance;

    @JsonProperty("Actions")
    private CommonVo actions = new CommonVo();

}
