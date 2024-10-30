package com.nextgen.gameaggregator.vendor.alize.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String error;
    private String description;
    private GameUrlDataVo data;

    @Override
    public String getGameUrl() {
        return this.data.getGameUrl();
    }
}
