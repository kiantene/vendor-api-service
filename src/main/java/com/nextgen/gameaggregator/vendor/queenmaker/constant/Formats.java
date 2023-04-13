package com.nextgen.gameaggregator.vendor.queenmaker.constant;

import org.springframework.http.MediaType;

public class Formats {

    // VIP Levels
    public static final Integer BRONZE = 1; // Bronze - basic limits
    public static final Integer SILVER = 2; // Silver - upgraded limits
    public static final Integer GOLD = 3; // Gold - high limits
    public static final Integer PLATINUM = 4; // Platinum - VIP limits

    // istestplayer
    public static final Boolean TEST_PLAYER = true; // istestplayer (true) = test player
    public static final Boolean REAL_PLAYER = false; // istestplayer (false) = real player

    // API Header
    public static final String APPLICATION_JSON = MediaType.APPLICATION_JSON.toString();
    public static final String HEADER_CLIENT_ID = "X-QM-ClientId";
    public static final String HEADER_CLIENT_SECRET = "X-QM-ClientSecret";
}
