package com.nextgen.gameaggregator.vendor.ambslot.api.betdetail;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    @NotNull(message = "url can not be blank")
    private DataVo data;

    @Override
    public String getBetDetailUrl() {
        return this.getData().getUrl();
    }
}
