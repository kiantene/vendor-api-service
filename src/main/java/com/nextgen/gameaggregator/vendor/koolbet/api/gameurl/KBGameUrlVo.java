package com.nextgen.gameaggregator.vendor.koolbet.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KBGameUrlVo implements GameUrlVo {

    @JsonProperty("Data")
    private String data;
    @JsonProperty("DisplayMode")
    private int displayMode;
    @JsonProperty("ErrorCode")
    private int errorCode;
    @JsonProperty("Message")
    private String message;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
