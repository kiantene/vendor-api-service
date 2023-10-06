package com.nextgen.gameaggregator.vendor.queenmaker.constant;

import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

public class Formats {

    // Wallet Code
    public static final String MAIN_WALLET_CODE = "MainWallet";

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

    // Transaction Types (txtype)
    public static final String PLACE_BET = "Place bet";
    public static final String WIN_BET = "Win bet";
    public static final String WIN_JACKPOT = "Win Jackpot";
    public static final String LOSE_BET = "Lose bet";
    public static final String FREE_BET = "Free bet";
    public static final String TIE_BET = "Tie bet";
    public static final String CANCEL_TRANSACTION = "Cancel transaction";
    public static final String END_ROUND = "End round";
    public static final String FUND_IN_THE_PLAYERS_WALLET = "Fund in the player's wallet";
    public static final String FUND_OUT_THE_PLAYERS_WALLET = "Fund out the player's wallet";
    public static final String CANCEL_FUNDOUT = "Cancel fund-out";

    public static final Map<String, Integer> TRANSACTION_TYPES = new LinkedHashMap<>() {{
        put(PLACE_BET, 500);
        put(WIN_BET, 510);
        put(WIN_JACKPOT, 511);
        put(LOSE_BET, 520);
        put(FREE_BET, 530);
        put(TIE_BET, 540);
        put(CANCEL_TRANSACTION, 560);
        put(END_ROUND, 590);
        put(FUND_IN_THE_PLAYERS_WALLET, 600);
        put(FUND_OUT_THE_PLAYERS_WALLET, 610);
        put(CANCEL_FUNDOUT, 611);
    }};

    public static final String REPLACE_STRING = "{replace}";

}
