package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.gameurl;

import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PGGameUrlVo implements GameUrlVo {

    @NotBlank(message = "url can not be blank")
    private String data;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
