package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PGGameUrlVo implements GameUrlVo {

    @NotBlank(message = "url can not be blank")
    private String data;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
