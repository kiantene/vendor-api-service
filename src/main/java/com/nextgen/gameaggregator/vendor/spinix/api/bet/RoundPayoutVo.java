package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class RoundPayoutVo implements HttpResponse {
    private Integer status;
    private RoundPayoutDataVo data;

    @Override
    public boolean hasError() {
        return false;
    }
}



