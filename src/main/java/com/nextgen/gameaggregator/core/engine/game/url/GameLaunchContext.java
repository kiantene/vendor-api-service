package com.nextgen.gameaggregator.core.engine.game.url;

import lombok.Builder;

@Builder
public class GameLaunchContext {
    private String token;
    private String gameCode;
    private Integer vendorId;
    private String vendorPlayerUsername;
    private String vendorCurrencyCode;
    private String vendorLanguageCode;
    private Integer platformId;
    private String lobbyUrl;
}
