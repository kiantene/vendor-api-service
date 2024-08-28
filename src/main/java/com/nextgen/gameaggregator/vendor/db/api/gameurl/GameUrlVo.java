package com.nextgen.gameaggregator.vendor.db.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    @NotBlank(message = "url can not be blank")
    private String data;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
