package com.nextgen.gameaggregator.vendor.evolutionlive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    @NotBlank(message = "url can not be blank")
    private String entry;
    private String entryEmbedded;
    private List<GameUrlErrorVo> errors;

    @Override
    public String getGameUrl() {
        return this.entry;
    }
}
