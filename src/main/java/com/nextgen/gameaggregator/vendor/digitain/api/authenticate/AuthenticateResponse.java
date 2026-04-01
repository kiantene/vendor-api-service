package com.nextgen.gameaggregator.vendor.digitain.api.authenticate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Builder
@Setter
public class AuthenticateResponse {

    private Integer err;

    private String tkn;

    private String pid;

    private String cid;

    private BigDecimal bln;

    private Boolean isr;


}
