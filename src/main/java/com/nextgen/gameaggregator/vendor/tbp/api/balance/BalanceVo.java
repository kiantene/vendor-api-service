package com.nextgen.gameaggregator.vendor.tbp.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceVo implements HttpResponse {

    @JsonProperty("Balance")
    private BigDecimal balance;

    @JsonProperty("ErrorCode")
    private int errorCode;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("Successful")
    private Boolean successful;

    @Override
    public boolean hasError() {
        return true;
    }
}
