package com.nextgen.gameaggregator.vendor.marblex.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Getter
@Setter
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDataVo {
    @JsonProperty("Balance")
    private BigDecimal balance;
    @JsonProperty("Currency")
    private String currency;
}
