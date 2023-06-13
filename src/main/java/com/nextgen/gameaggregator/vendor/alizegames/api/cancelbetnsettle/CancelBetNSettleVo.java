package com.nextgen.gameaggregator.vendor.alizegames.api.cancelbetnsettle;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.vendor.alizegames.vo.ResponseVo;

import lombok.Data;

@Data
public class CancelBetNSettleVo extends ResponseVo {
    private BigDecimal balance;
    private String username;
    private String currency;
    private Long timestamp;
}
