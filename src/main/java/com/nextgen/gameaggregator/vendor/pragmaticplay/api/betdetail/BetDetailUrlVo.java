package com.nextgen.gameaggregator.vendor.pragmaticplay.api.betdetail;

import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private String error;
    private String description;
    @NotBlank(message = "url can not be blank")
    private String url;

    @Override
    public String getBetDetailUrl() { return this.getUrl(); }
}
