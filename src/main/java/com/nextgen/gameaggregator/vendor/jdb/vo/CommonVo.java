package com.nextgen.gameaggregator.vendor.jdb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommonVo implements HttpResponse {
    @JsonProperty("status")
    private String status;
    @JsonProperty("balance")
    private BigDecimal balance;
    @JsonProperty("err_text")
    private String errText;

    @Override
    public boolean hasError() {
        return false;
    }
}
