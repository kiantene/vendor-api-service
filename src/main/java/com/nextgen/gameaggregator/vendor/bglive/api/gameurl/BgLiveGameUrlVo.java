package com.nextgen.gameaggregator.vendor.bglive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BgLiveGameUrlVo implements GameUrlVo {

    @JsonProperty("result")
    private String result;

    @JsonProperty("error")
    private ErrorDto error;

    @Override
    public String getGameUrl() {
        return result;
    }

    public boolean isSuccess() {
        return result != null && !result.isBlank();
    }
}
