package com.nextgen.gameaggregator.vendor.dreamgaming.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberVo implements HttpResponse {
    private String username;

    private BigDecimal password;

    private String currency;

    private BigDecimal winLimit;

    private BigDecimal balance;

    private BigDecimal amount;

    @Override
    public boolean hasError() {
        return false;
    }
}
