package com.nextgen.gameaggregator.vendor.groove.api.betdetail;

import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrooveBetDetailUrlVo implements BetDetailUrlVo {
    private String url = "";

    @Override
    public String getBetDetailUrl() {
        return url;
    }
}