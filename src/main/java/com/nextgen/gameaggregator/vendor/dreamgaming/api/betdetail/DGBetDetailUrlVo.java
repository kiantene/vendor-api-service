package com.nextgen.gameaggregator.vendor.dreamgaming.api.betdetail;

import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DGBetDetailUrlVo implements BetDetailUrlVo {
    private String url = "";

    @Override
    public String getBetDetailUrl() {
        return url;
    }
}
