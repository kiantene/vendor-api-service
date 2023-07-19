package com.nextgen.gameaggregator.vendor.evoplay.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDataVo {

    private BigDecimal balance;
    private String currency;
    private String scope;
    private Integer no_refund;
    private String message;
}