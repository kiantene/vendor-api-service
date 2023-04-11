package com.nextgen.gameaggregator.vendor.qm.constant;

import java.util.Arrays;
import java.util.List;

public class GameType {
    public static final String SLOT = "1"; // 1 电子
    public static final String POKER = "2"; // 2 棋牌
    public static final String LOBBY = "3"; // 3 游戏大厅
    public static final String FISH = "5"; // 5 捕鱼
    public static final String ARCADE = "8"; // 8 押分机 (含宾果)

    public static final List<String> GameTypeList = Arrays.asList(SLOT, FISH, POKER, LOBBY, ARCADE);
}
