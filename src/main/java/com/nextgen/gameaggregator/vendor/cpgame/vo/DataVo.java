package com.nextgen.gameaggregator.vendor.cpgame.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataVo {
    private BigDecimal balance;

    private String currency;

    @JsonProperty("update_ms")
    private Long updatedMs;
}
