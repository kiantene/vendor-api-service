package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.betdetail;

import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo;
import lombok.Data;

@Data
public class PGBetDetailUrlVo implements BetDetailUrlVo {

    private String url = "";

    @Override
    public String getBetDetailUrl() {
        return this.getUrl();
    }
}
