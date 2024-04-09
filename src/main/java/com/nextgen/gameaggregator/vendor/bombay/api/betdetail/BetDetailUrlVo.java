package com.nextgen.gameaggregator.vendor.bombay.api.betdetail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    @NotBlank(message = "url can not be blank")
    private String url = "";

    @Override
    public String getBetDetailUrl() {
        return this.getUrl();
    }
}
