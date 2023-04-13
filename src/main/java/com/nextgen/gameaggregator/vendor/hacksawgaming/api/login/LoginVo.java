package com.nextgen.gameaggregator.vendor.hacksawgaming.api.login;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.hacksawgaming.api.gameurl.GameUrlVo;
import com.nextgen.gameaggregator.vendor.spinix.api.balance.BalanceDataVo;
import com.nextgen.gameaggregator.vendor.spinix.api.balance.BalanceErrorVo;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LoginVo implements HttpResponse {

    private Integer code;
    private String msg;
    private LoginDataVo data;

    @Override
    public boolean hasError() {
        return false;
    }

}



