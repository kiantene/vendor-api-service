package com.nextgen.gameaggregator.vendor.jili.api.cancelsessionbet;

import com.nextgen.gameaggregator.vendor.jili.vo.ResponseVo;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
@Data
public class CancelSessionBetVo extends ResponseVo {
    private String username;      // Identifier of the user within the Casino Operator’s system
    private String currency;    // Currency of the player
    private BigDecimal balance;    // Real balance of the player
    private BigInteger txId;
}
