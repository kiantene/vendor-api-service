package com.nextgen.gameaggregator.vendor.kypoker.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GameUrlDataVo {

    @JsonProperty("game_url")
    private String gameUrl;

    @JsonProperty("token")
    private String token;

}
