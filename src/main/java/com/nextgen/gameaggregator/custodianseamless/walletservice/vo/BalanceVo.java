package com.nextgen.gameaggregator.custodianseamless.walletservice.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceVo extends ResponseVo {
    private String username;
    private Integer tokenId;
    private BigDecimal balance;
}

