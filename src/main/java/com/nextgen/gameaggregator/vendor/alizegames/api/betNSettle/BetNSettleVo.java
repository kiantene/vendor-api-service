package com.nextgen.gameaggregator.vendor.alizegames.api.betNSettle;

import com.nextgen.gameaggregator.vendor.alizegames.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class BetNSettleVo extends ResponseVo {
    private String username;    // Identifier of the user within the Casino Operator’s system
    private String currency;    // Currency of the player
    private String token;       // Token/session of the player
    private String operatorId;
    private Long timestamp;
    private BigDecimal balance;
}
