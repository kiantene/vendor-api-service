package com.nextgen.gameaggregator.vendor.advantplay.api.betdetail;

import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private String gameUrl;

    public BetDetailUrlVo(String gameUrl) {
        this.gameUrl = gameUrl;
    }

    @Override
    public String getBetDetailUrl() {
        return this.gameUrl;
    }
}
