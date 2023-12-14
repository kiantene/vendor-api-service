package com.nextgen.gameaggregator.vendor.advantplay.api.gameurl;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    @NotBlank(message = "url can not be blank")
    private String url;

    @Override
    public String getGameUrl() {
        return url;
    }

}