package com.nextgen.gameaggregator.vendor.dotconnections.api.betdetail;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private Integer code;
    private String msg;

    @NotNull(message = "url cannot be blank")
    private UrlVo data;

    @Override
    public String getBetDetailUrl() {
        return data.getRecord();
    }
}
