package com.nextgen.gameaggregator.vendor.pragmaticplay.api.betdetail;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    private String error;
    private String description;
    private String url;
    @NotBlank(message = "test can not be blank")
    private String test;

    @Override
    public String getBetDetailUrl() { return this.getUrl(); }
}
