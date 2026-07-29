package com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDataVo {

    private BigDecimal balance;
    private String currency;
    private String scope;
    private Integer no_refund;
    private String message;
}