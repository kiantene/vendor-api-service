package com.nextgen.gameaggregator.vendor.alizegames.api.gameurl;

import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String error;
    private String description;
    private GameUrlDataVo data;

    @Override
    public String getGameUrl() {
        return this.data.getGameUrl();
    }
}
