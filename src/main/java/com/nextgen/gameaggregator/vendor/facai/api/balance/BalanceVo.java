package com.nextgen.gameaggregator.vendor.facai.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceVo implements HttpResponse {

    @JsonProperty("Result")
    private Integer result;
    @JsonProperty("MainPoints")
    private BigDecimal mainPoints;
    @JsonProperty("ErrorText")
    private String errorText;

    @Override
    public boolean hasError() {
        return false;
    }

}
