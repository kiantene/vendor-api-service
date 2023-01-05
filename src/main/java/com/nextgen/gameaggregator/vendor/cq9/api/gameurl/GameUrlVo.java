package com.nextgen.gameaggregator.vendor.cq9.api.gameurl;

import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String url;
    private String token;

    @Override
    public String getGameUrl() {
        return this.getUrl();
    }
}
