package com.nextgen.gameaggregator.vendor.ezugi.api.authentication;

import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AuthenticationVo extends CommonVo {
    private String uid;
    private Double balance;
    private String currency;
}
