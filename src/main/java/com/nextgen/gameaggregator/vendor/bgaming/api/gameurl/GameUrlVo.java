package com.nextgen.gameaggregator.vendor.bgaming.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    @NotNull
    @JsonProperty("launch_options")
    private LaunchOptionsVo launchOptionsVo;
    @JsonProperty("session_id")
    private String sessionId;

    @Override
    public String getGameUrl() {
        return this.getLaunchOptionsVo().getGameUrl();
    }
}