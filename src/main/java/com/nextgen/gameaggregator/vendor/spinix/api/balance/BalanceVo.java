package com.nextgen.gameaggregator.vendor.spinix.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BalanceVo implements HttpResponse {

    private String reqId;
    private Integer status;
    private BalanceDataVo data;
    private BalanceErrorVo error;

    @Override
    public boolean hasError() {
        return false;
    }

}



