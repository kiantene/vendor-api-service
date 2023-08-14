package com.nextgen.gameaggregator.vendor.bgaming.api.gameurl;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LaunchOptionsVo {
    @NotBlank
    @JsonProperty("game_url")
    private String gameUrl;
    private String strategy;
}
