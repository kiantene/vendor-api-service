package com.nextgen.gameaggregator.vendor.mg.api.balance;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetBalanceVo implements HttpResponse {
    private String currency;
    private BigDecimal balance;

    @Override
    public boolean hasError() {
        return false;
    }
}
