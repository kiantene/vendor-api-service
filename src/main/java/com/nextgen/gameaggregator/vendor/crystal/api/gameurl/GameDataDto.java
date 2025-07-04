package com.nextgen.gameaggregator.vendor.crystal.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDataDto {
    @JsonProperty("url")
    private String url;
}
