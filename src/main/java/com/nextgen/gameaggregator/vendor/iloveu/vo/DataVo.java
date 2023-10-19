package com.nextgen.gameaggregator.vendor.iloveu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataVo {

    @JsonProperty("Balance")
    private BigDecimal balance;
}
