package com.nextgen.gameaggregator.vendor.jili.api.betdetail;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private Integer ErrorCode;
    private String Message;
    @NotNull(message = "Data contain url cannot be null")
    private UrlVo Data;

    @Override
    public String getBetDetailUrl() {
        return this.Data.getUrl();
    }
}
