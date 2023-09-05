package com.nextgen.gameaggregator.vendor.alize.constant;

import java.util.HashMap;
import java.util.Map;

public class GameId {
    private static final Map<String, String> gameCodeToId = new HashMap<>();

    static {
        gameCodeToId.put("plinko", "1");
        gameCodeToId.put("crash", "2");
        gameCodeToId.put("hotpot", "3");
        gameCodeToId.put("keno", "4");
        gameCodeToId.put("steampunk", "5");
        gameCodeToId.put("coin", "6");
        gameCodeToId.put("hilo", "7");
        gameCodeToId.put("dice", "8");
        gameCodeToId.put("vecarz", "9");
        gameCodeToId.put("double", "10");
    }

    public static String getGameId(String vendorGameCode) {
        String gameId = gameCodeToId.get(vendorGameCode.toLowerCase()); // Convert to lowercase for case-insensitive lookup
        if (gameId != null) {
            return gameId;
        }
        // Handle the case when vendorGameCode is not found
        return "0"; // Default value
    }
}
