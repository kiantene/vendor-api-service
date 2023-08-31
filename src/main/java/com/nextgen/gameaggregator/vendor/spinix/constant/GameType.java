package com.nextgen.gameaggregator.vendor.spinix.constant;

import java.util.Arrays;
import java.util.List;

public class GameType {
    public static final String SLOT = "slots";
    public static final String FISH = "fish";
    public static final String TABLE = "tables";
    public static final String CRASH = "crash";
    public static final String ARCADE = "arcade";
    public static final String ARCADE_BINGO = "arcade_bingo";

    public static final List<String> BET_AMOUNT_GAME_TYPE_LIST = Arrays.asList(TABLE, ARCADE, ARCADE_BINGO);
}
