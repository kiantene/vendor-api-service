package com.nextgen.gameaggregator.vendor.bombay.api.gameurl;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo{

    @NotNull(message = "url can not be blank")
    private String url;

    @Override
    public String getGameUrl() {
        return this.getUrl();
    }
}
