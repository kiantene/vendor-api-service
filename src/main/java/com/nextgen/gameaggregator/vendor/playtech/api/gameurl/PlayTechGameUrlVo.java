package com.nextgen.gameaggregator.vendor.playtech.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayTechGameUrlVo implements GameUrlVo {

    @NotBlank(message = "url can not be blank")
    private DataVo data;

    @Override
    public String getGameUrl() {
        return this.data.getUrl();
    }
}
