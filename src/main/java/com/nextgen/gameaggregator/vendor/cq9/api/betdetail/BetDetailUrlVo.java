package com.nextgen.gameaggregator.vendor.cq9.api.betdetail;

import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    private String data;
    private StatusVo status;

    @Override
    public String getBetDetailUrl() {
        return data;
    }
}
