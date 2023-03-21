package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

public class RoundPayoutVo implements HttpResponse {
    private String reqId;
    private Integer status;
    private RoundPayoutDataVo data;
    private RoundPayoutErrorVo error;

    @Override
    public boolean hasError() {
        return false;
    }
}



