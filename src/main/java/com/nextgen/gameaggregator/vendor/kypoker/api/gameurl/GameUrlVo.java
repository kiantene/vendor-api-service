package com.nextgen.gameaggregator.vendor.kypoker.api.gameurl;

import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    private String url;

    @Override
    public String getGameUrl() {

        return url;

    }
}
