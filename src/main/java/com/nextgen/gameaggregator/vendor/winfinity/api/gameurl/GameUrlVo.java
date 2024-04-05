package com.nextgen.gameaggregator.vendor.winfinity.api.gameurl;

import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private GameUrlDataVo data;

    @Override
    public String getGameUrl() {
        return this.data.getFrameUrl();
    }
}
