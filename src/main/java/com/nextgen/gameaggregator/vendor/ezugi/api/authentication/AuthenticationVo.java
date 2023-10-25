package com.nextgen.gameaggregator.vendor.ezugi.api.authentication;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationVo extends CommonVo {
    private String uid;
    private BigDecimal balance;
    private String currency;
}
