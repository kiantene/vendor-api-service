package com.nextgen.gameaggregator.vendor.inout.api.betdetail;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    @NotNull(message = "url cannot be blank")
    private String url;

    @Override
    public String getBetDetailUrl() {
        return this.getUrl();
    }
}
