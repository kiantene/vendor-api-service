package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.betdetail;

import lombok.Data;

@Data
public class PGBetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    private String url = "";

    @Override
    public String getBetDetailUrl() {
        return this.getUrl();
    }
}
