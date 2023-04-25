package com.nextgen.gameaggregator.vendor.hacksawgaming.api.betdetail;

import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private Integer code;
    private String msg;
    private UrlVo data;

    @Override
    public String getBetDetailUrl() { return data.getRecord(); }
}
