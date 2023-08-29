package com.nextgen.gameaggregator.vendor.habanero.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo{

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("currencycode")
    private String currencyCode;
}
