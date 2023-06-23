package com.nextgen.gameaggregator.vendor.habanero.api.betdetail;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    @JsonProperty("Result")
    private Integer result;

    @JsonProperty("Url")
    @NotBlank(message = "url can not be blank")
    private String url;

    @Override
    public String getBetDetailUrl() { return this.getUrl(); }
}