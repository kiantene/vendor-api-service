package com.nextgen.gameaggregator.core.engine.game.url;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class GameLaunchContext {
    private String vendorClassName;
    private String token;
    private String gameCode;
    private Integer vendorId;
    private String vendorPlayerUsername;
    private String vendorCurrencyCode;
    private String vendorLanguageCode;
    private Integer platformId;
    private String lobbyUrl;

    Map<String, String> vendorCredentials;

    private String vendorToken;
    private String gameUrl;
}
