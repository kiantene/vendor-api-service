package com.nextgen.gameaggregator.vendor.pragmaticplay.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateVo extends ResponseVo {
    private String userId;      // Identifier of the user within the Casino Operator’s system
    private String currency;    // Currency of the player
    private BigDecimal cash;    // Real balance of the player
    private BigDecimal bonus;   // Bonus balance of the player
}
