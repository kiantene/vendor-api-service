package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetVo implements HttpResponse {

    private Integer Result;
    private Double MainPoints;
    private String ErrorText;

    @Override
    public boolean hasError() {
        return false;
    }
}
