package com.nextgen.gameaggregator.vendor.mg.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    @NotBlank
    private String url;

    @Override
    public String getGameUrl() {
        return url;
    }
}
