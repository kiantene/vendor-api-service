package com.nextgen.gameaggregator.vendor.poker365.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Poker365GameUrlVo implements GameUrlVo {
    @NotBlank(message = "url can not be blank")
    private String url;

    @Override
    public String getGameUrl() {
        return this.url;
    }
}

