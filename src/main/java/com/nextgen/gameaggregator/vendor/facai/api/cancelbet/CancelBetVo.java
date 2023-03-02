package com.nextgen.gameaggregator.vendor.facai.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelBetVo implements HttpResponse {

    private Integer Result;
    private Double MainPoints;
    private String ErrorText;

    @Override
    public boolean hasError() {
        return false;
    }
}
