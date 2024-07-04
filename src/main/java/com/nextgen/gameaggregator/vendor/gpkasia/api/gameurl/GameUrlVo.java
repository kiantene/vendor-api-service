package com.nextgen.gameaggregator.vendor.gpkasia.api.gameurl;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo{

    @NotBlank(message = "url can not be blank")
    private String data;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
