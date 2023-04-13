package com.nextgen.gameaggregator.vendor.queenmaker.api.gameurl;

import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String gameUrl;

    public GameUrlVo(String gameUrl) {
        this.gameUrl = gameUrl;
    }

    @Override
    public String getGameUrl() { return this.gameUrl; }
}
