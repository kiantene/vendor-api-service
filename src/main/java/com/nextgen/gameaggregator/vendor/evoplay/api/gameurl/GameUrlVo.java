package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import lombok.Data;

import java.util.Optional;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String status;
    private DataVo data;
    private ErrorVo error;

    @Override
    public String getGameUrl() {
        return Optional.ofNullable(this.getData())
                .map(DataVo::getLink)   // If getData() is not null, call getLink()
                .orElse(null);    // If getData() is null, return null
    }
}
