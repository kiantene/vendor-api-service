package com.nextgen.gameaggregator.vendor.saba.api.balance;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetBalanceVo implements HttpResponse {
    private String status;
    private String userId;
    private BigDecimal balance;
    private String balanceTs;
    private String msg;

    @Override
    public boolean hasError() {
        return !this.status.equals("0");
    }
}
