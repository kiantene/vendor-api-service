package com.nextgen.gameaggregator.vendor.alizegames.api.betNSettle;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.vendor.alizegames.vo.ResponseVo;

import lombok.Data;

@Data
public class BetNSettleVo extends ResponseVo {
    private BigDecimal balance;
    private String username;
    private String currency;
    private Long timestamp;
}
