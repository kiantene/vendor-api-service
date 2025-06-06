package com.nextgen.gameaggregator.vendor.spadegaming.api.betdetail;

import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private String ticketUrl = "";

    @Override
    public String getBetDetailUrl() {
        return ticketUrl;
    }
}
