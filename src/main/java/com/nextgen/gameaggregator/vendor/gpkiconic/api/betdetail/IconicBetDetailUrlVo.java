package com.nextgen.gameaggregator.vendor.gpkiconic.api.betdetail;

import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo;
import lombok.Data;

@Data
public class IconicBetDetailUrlVo implements BetDetailUrlVo {
    private String url = "";

    @Override
    public String getBetDetailUrl() {
        return this.getUrl();
    }
}
