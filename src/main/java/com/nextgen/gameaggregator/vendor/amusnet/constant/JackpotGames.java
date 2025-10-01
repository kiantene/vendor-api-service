package com.nextgen.gameaggregator.vendor.amusnet.constant;

import java.util.Set;

public class JackpotGames {
    //jackpot game, dun have inside database
    private static final Set<String> EXCLUDED_GAME_IDS = Set.of("996", "998", "999", "8888");

    public static boolean isJackpotGame(String gameId) {
        return EXCLUDED_GAME_IDS.contains(gameId);
    }
}
