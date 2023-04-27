package com.nextgen.gameaggregator.vendor.jili.api.gameurl;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private Integer ErrorCode;
    private String Message;
    @NotBlank(message = "url can not be blank")
    private String Data;

    @Override
    public String getGameUrl() {
        return this.Data;
    }
}
