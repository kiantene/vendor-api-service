package com.nextgen.gameaggregator.vendor.queenmaker.constant;

import java.util.Arrays;
import java.util.List;

public class Txtype {
    public static final Integer PLACE_BET = 500; // 投注(Place bet)
    public static final Integer WIN_BET = 510; //赢钱(Win bet)
    public static final Integer WIN_JACKPOT = 511; //贏彩金(Win Jackpot)
    public static final Integer LOSE_BET = 520; //输钱(Lose bet) 未使用。 0值表示交易未送出。
    public static final Integer FREE_BET = 530; //免费投注(Free bet)
    public static final Integer TIE_BET = 540; //平手(Tie bet)
    public static final Integer CANCEL_BET = 560; //取消交易(Cancel bet)
    public static final Integer END_ROUND = 590; //结束局(End Round)
    public static final Integer FUND_IN = 600; //电子钱包加钱 (Fund in the player’s wallet)
    public static final Integer FUND_OUT = 610; //电子钱包扣钱 (Fund out the player’s wallet)
    public static final Integer CANCEL_FUND = 611; //取消电子钱包扣钱 (Cancel fund out)

    public static final List<Integer> txtTypeList = Arrays.asList(
            PLACE_BET,
            WIN_BET,
            WIN_JACKPOT,
            LOSE_BET,
            FREE_BET,
            TIE_BET,
            CANCEL_BET,
            END_ROUND,
            FUND_IN,
            FUND_OUT,
            CANCEL_FUND);

}
