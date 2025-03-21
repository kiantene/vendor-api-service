package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.betdetail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private String error;
    private String description;
    @NotBlank(message = "url can not be blank")
    private String url;

    @Override
    public String getBetDetailUrl() {
        return this.getUrl();
    }
}
