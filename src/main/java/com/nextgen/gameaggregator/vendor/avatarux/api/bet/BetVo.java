package com.nextgen.gameaggregator.vendor.avatarux.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.avatarux.vo.ErrorVo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetVo implements HttpResponse {
    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("error")
    private ErrorVo error;

    @Override
    public boolean hasError() {
        return true;
    }
}