package com.nextgen.gameaggregator.game.launcher.casinogate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class GameLaunchRequest {
    private Map<String, Object> buffer;
    private String cashierUrl;
    private String casinoId;
    private String countryCode;
    private String currency;
    private boolean demo;
    private String gameId;
    private String ipAddress;

    @JsonProperty("isMobile")
    private boolean mobile;

    private String language;
    private String lobbyUrl;
    private String token;
    private String userId;
}
