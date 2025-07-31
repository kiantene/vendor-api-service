package com.nextgen.gameaggregator.vendor.saba.api.resettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtraInfo {
    @JsonProperty("isOnlyWinlostDateChanged")
    private boolean isOnlyWinLostDateChanged;
}
