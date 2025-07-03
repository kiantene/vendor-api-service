package com.nextgen.gameaggregator.vendor.crystal.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class CommonVo {
    private BigDecimal balance;

}
