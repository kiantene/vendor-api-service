package com.nextgen.gameaggregator.vendor.saba.api.gameurl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    @JsonProperty("error_code")
    private Integer errorCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("Data")
    private String data;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
