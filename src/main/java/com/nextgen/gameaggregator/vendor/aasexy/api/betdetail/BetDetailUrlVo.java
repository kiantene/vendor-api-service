package com.nextgen.gameaggregator.vendor.aasexy.api.betdetail;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private Integer statusCode;
    private String statusMessage;

    @NotNull(message = "url cannot be blank")
    private String url;

    @Override
    public String getBetDetailUrl() {
        return this.getUrl();
    }
}
