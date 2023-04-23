package com.nextgen.gameaggregator.vendor.pragmaticplay.api.gameurl;

import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String error;
    private String description;
    private String gameURL;

    @Override
    public String getGameUrl() {
        return this.gameURL;
    }
}
