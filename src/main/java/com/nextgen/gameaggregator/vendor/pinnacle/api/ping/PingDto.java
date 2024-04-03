package com.nextgen.gameaggregator.vendor.pinnacle.api.ping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PingDto {
    @JsonProperty("Timestamp")
    private String timestamp;
}
