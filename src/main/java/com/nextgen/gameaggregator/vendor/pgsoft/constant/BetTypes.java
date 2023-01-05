package com.nextgen.gameaggregator.vendor.pgsoft.constant;

public class BetTypes {
    // REQUEST_AND_WIN (has matching symbols, and ongoing next)
    public static final String REQUEST_AND_WIN_AND_ONGOING = "1";
    // REQUEST_AND_WIN (has matching symbols, end round here)
    public static final String REQUEST_AND_WIN_AND_END_ROUND = "2";
    // REQUEST_AND_LOSE (no matching symbols, end round here)
    public static final String REQUEST_AND_LOSE_AND_END_ROUND = "3";
    // END_ROUND (no matching symbols, end round here)
    public static final String END_ROUND = "4";
    // FREESPIN/MATCHING_SYMBOLS WIN_AND_ONGOING (has matching symbols, and ongoing next)
    public static final String FREESPIN_WIN_AND_ONGOING = "5";
    // FREESPIN/MATCHING_SYMBOLS LOSE_AND_ONGOING (no matching symbols, and ongoing next)
    public static final String FREESPIN_LOSE_AND_ONGOING = "6";
    public static final String FREESPIN = "7";
    // UNIDENTIFIABLE
    public static final String UNIDENTIFIABLE = "0";
}