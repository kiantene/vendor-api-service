package com.nextgen.gameaggregator.vendor.spinix.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class BalanceVo implements HttpResponse {

    private Integer status;
    private BalanceDataVo data;

    @Override
    public boolean hasError() {
        return false;
    }

}



