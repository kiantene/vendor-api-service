package com.nextgen.gameaggregator.vendor.dotconnections.api.gameurl;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    @SerializedName("game_url")
    private String gameUrl;

    @Override
    public String getGameUrl() {
        return this.gameUrl;
    }
}
