package com.nextgen.gameaggregator.vendor.spinix.api.betdetail;

import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private String reqId;
    private String status;
    private UrlVo data;

    @Override
    public String getBetDetailUrl() { return data.getUrl(); }
}
