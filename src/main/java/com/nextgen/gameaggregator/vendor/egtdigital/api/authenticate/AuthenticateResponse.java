package com.nextgen.gameaggregator.vendor.egtdigital.api.authenticate;

import com.nextgen.gameaggregator.vendor.egtdigital.vo.ResponseCommonVo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@Setter
@EqualsAndHashCode(callSuper = true)
public class AuthenticateResponse extends ResponseCommonVo {

    private String currency;
    
}
