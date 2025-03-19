package com.nextgen.gameaggregator.vendor.amusnet.api.betdetail;

import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    private String url = "";

    @Override
    public String getBetDetailUrl() {
        return this.getUrl();
    }
}
