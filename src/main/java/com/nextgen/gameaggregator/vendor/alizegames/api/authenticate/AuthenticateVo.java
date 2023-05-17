package com.nextgen.gameaggregator.vendor.alizegames.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.alizegames.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateVo extends ResponseVo {
    private String username;    // Identifier of the user within the Casino Operator’s system
    private String currency;    // Currency of the player
    private String token;       // Token/session of the player
    private String operatorId;
    private Long timestamp;
}
