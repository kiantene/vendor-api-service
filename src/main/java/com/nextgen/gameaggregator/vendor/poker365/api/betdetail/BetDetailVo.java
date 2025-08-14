package com.nextgen.gameaggregator.vendor.poker365.api.betdetail;

import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo;
import lombok.Data;

@Data
public class BetDetailVo implements BetDetailUrlVo {
    private String data;

    @Override
    public String getBetDetailUrl() {
        return data;
    }
}
