package com.nextgen.gameaggregator.vendor.smartsoft.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetVo implements HttpResponse {
    @JsonProperty("TransactionId")
    private String transactionId;

    @JsonProperty("Balance")
    private BigDecimal balance;

    @Override
    public boolean hasError() {
        return false;
    }
}