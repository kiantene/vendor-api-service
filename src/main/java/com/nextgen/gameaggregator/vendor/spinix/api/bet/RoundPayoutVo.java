package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoundPayoutVo implements HttpResponse {
    private Integer status;
    private RoundPayoutDataVo data;
    private RoundPayoutErrorVo error;

    @Override
    public boolean hasError() {
        return false;
    }
}



