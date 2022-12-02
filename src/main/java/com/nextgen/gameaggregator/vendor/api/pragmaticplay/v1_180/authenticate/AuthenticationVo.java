package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.authenticate;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.vo.AbstractActionVo;
import lombok.Data;

@Data
public class AuthenticationVo extends AbstractActionVo {
    private String userId;
    private String currency;
    private Double cash;
    private Double bonus;
    private String token;
}
