package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String status;
    private DataVo data;

    @Override
    public String getGameUrl() {
        return this.getData().getLink();
    }
}
