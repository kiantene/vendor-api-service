package com.nextgen.gameaggregator.vendor.pgsoft.api.betdetail;

import lombok.Data;

@Data
public class BetDetailUrlVo  implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private String error;
    private String url = null;

    @Override
    public String getBetDetailUrl() { return this.getUrl(); }
}
