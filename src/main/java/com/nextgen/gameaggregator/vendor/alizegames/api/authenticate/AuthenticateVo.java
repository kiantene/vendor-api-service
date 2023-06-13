package com.nextgen.gameaggregator.vendor.alizegames.api.authenticate;

import com.nextgen.gameaggregator.vendor.alizegames.vo.ResponseVo;

import lombok.Data;

@Data
public class AuthenticateVo extends ResponseVo {
    private String token;
    private String username;
    private String currency;
    private String operatorId;
    private Long timestamp;
}
