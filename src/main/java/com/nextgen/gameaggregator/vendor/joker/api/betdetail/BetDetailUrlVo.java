package com.nextgen.gameaggregator.vendor.joker.api.betdetail;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.*;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    @JsonProperty("Error")
    private String error;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Url")
    @NotBlank(message = "url can not be blank")
    private String url;

    public String getUrl() { return "https:" + this.url; }

    @Override
    public String getBetDetailUrl() { return this.getUrl(); }
}
