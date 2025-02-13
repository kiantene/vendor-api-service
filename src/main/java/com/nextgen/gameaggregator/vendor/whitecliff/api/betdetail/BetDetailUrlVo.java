package com.nextgen.gameaggregator.vendor.whitecliff.api.betdetail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {

    @JsonProperty("url")
    @NotBlank(message = "url can not be blank")
    private String url;

    @JsonIgnore
    private String error;

    @NotNull
    private Integer status;

    @Override
    public String getBetDetailUrl() {
        return this.url;
    }
}
