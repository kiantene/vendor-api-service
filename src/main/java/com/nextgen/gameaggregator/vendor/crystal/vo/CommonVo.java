package com.nextgen.gameaggregator.vendor.crystal.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CommonVo {
    private BigDecimal balance;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String actionId;

}
