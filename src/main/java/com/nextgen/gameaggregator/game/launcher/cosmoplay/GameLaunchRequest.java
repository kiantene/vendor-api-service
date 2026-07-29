package com.nextgen.gameaggregator.game.launcher.cosmoplay;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameLaunchRequest {
    private String partnerCode;

    //gameId
    @NotBlank
    private String gid;

    //playerId
    @NotBlank
    private String pid;

    //Authtoken
    @NotBlank
    private String atk;

    private String language;

    // Game laucher URL
    private String url;

    private String sd;
}
