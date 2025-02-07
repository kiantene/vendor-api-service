package com.nextgen.gameaggregator.vendor.bglive.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    
    @JsonProperty("params")
    private ParamsDto params;

}
