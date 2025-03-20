package com.nextgen.gameaggregator.vendor.playtech.api.betdetail;

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

