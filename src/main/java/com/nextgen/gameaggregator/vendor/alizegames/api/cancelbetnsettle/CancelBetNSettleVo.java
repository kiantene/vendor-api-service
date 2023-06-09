package com.nextgen.gameaggregator.vendor.alizegames.api.cancelbetnsettle;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.vendor.alizegames.vo.ResponseVo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CancelBetNSettleVo extends ResponseVo {
    private String username;
    private String currency;
    private Long timestamp;
    private BigDecimal balance;
}
