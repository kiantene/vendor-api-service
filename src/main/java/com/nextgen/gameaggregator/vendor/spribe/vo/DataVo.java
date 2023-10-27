package com.nextgen.gameaggregator.vendor.spribe.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataVo {

  private String user_id;
  private String username;
  private BigDecimal balance;
  private String currency;
  private String operator_tx_id;
  private BigDecimal new_balance;
  private BigDecimal old_balance;
  private String provider;
  private String provider_tx_id;
}

