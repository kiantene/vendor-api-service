package com.nextgen.gameaggregator.vendor.tbp.api.betdetail;

import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TBPBetDetailUrlVo implements BetDetailUrlVo {
    private String url = "";

    @Override
    public String getBetDetailUrl() {
        return url;
    }
}
