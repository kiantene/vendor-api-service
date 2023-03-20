package com.nextgen.gameaggregator.vendor.jili.api.sessionbet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.jili.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionBetVo extends ResponseVo {
    private String username;      // Identifier of the user within the Casino Operator’s system
    private String currency;    // Currency of the player
    private BigDecimal balance;    // Real balance of the player
    private String token;       // Token/session of the player
}
