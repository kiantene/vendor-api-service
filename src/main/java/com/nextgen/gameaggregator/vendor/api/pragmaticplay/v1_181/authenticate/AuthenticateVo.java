package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.vo.AbstractActionVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AuthenticateVo {
    // Identifier of the user within the Casino Operator’s system
    private String userId;

    // Currency of the player
    private String currency;

    // Real balance of the player
    private BigDecimal cash;

    // Bonus balance of the player
    private BigDecimal bonus;

    // Response status
    private Integer error;

    // Response status short description
    private String description;

//    private String traceId;
//    private String token;
//    private String errorCheck;
}
