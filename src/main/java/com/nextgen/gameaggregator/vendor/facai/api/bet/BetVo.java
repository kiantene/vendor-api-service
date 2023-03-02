package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetVo implements HttpResponse {

    @JsonProperty("Result")
    private Integer result;
    @JsonProperty("MainPoints")
    private Double mainPoints;
    @JsonProperty("ErrorText")
    private String errorText;


    @Override
    public boolean hasError() {
        return false;
    }
}
