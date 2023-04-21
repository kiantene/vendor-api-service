package com.nextgen.gameaggregator.vendor.facai.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    @JsonProperty("Result")
    private Integer result;
    @JsonProperty("Url")
    private String url;

    @Override
    public String getGameUrl() {
        return this.getUrl();
    }
}
