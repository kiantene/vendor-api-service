package com.nextgen.gameaggregator.vendor.pragmaticplay.api.betdetail;

import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    private String error;
    private String description;
    private String url;

    @Override
    public String getBetDetailUrl() { return this.getUrl(); }
}
