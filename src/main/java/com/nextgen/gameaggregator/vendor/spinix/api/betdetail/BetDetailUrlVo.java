package com.nextgen.gameaggregator.vendor.spinix.api.betdetail;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private String reqId;
    private String status;

    @NotNull(message = "url cannot be blank")
    private UrlVo data;

    @Override
    public String getBetDetailUrl() {
        return data.getUrl();
    }
}
