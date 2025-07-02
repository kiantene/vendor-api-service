package com.nextgen.gameaggregator.vendor.cq9.constant;

import java.util.Arrays;
import java.util.List;

public class GameType {
    public static final String SLOT = "slot";
    public static final String FISH = "fish";
    public static final String TABLE = "table";
    public static final String LIVEGAME = "livegame";
    public static final String ARCADE = "arcade";
    public static final String DEALERGAME = "dealergame";
    public static final String ANIMAL = "animal";

    public static final List<String> GameTypeList = Arrays.asList(SLOT, FISH, TABLE, LIVEGAME, ARCADE, DEALERGAME, ANIMAL);
}
