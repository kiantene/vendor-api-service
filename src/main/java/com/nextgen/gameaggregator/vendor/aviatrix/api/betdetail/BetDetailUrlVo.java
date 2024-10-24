package com.nextgen.gameaggregator.vendor.aviatrix.api.betdetail;

import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    private String data;

    @Override
    public String getBetDetailUrl() {
        return data;
    }
}
