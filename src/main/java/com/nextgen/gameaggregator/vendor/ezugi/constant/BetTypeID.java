package com.nextgen.gameaggregator.vendor.ezugi.constant;

import java.util.HashMap;
import java.util.Map;

public class BetTypeID {
    public static final int DEBIT_NORMAL = 1;
    public static final int DEBIT_TIP = 3;
    public static final int DEBIT_INSURANCE = 4;
    public static final int DEBIT_DOUBLE = 5;
    public static final int DEBIT_SPLIT = 6;
    public static final int DEBIT_SWAP = 8;
    public static final int DEBIT_BUY_PLAYER_CARD = 9;
    public static final int DEBIT_ROYAL_POKER_INSURANCE = 10;
    public static final int DEBIT_BUY_DEALER_CARD = 11;
    public static final int DEBIT_CALL = 24;
    public static final int CREDIT_NORMAL = 101;
    public static final int CREDIT_TIP = 103;
    public static final int CREDIT_INSURANCE = 104;
    public static final int CREDIT_DOUBLE = 105;
    public static final int CREDIT_SPLIT = 106;
    public static final int CREDIT_SWAP = 108;
    public static final int CREDIT_BUY_PLAYER_CARD = 109;
    public static final int CREDIT_ROYAL_POKER_INSURANCE = 110;
    public static final int CREDIT_BUY_DEALER_CARD = 111;
    public static final int CREDIT_CALL = 124;

    public static final Map<Integer, String> VALID_DEBIT_BET_TYPE_ID = new HashMap<>() {{
        put(DEBIT_NORMAL, "normal");
        put(DEBIT_TIP, "tip");
        put(DEBIT_INSURANCE, "insurance");
        put(DEBIT_DOUBLE, "double");
        put(DEBIT_SPLIT, "split");
        put(DEBIT_SWAP, "swap");
        put(DEBIT_BUY_PLAYER_CARD, "buy player card");
        put(DEBIT_ROYAL_POKER_INSURANCE, "royal poker insurance");
        put(DEBIT_BUY_DEALER_CARD, "buy dealer card");
        put(DEBIT_CALL, "call");
    }};

    public static final Map<Integer, String> VALID_CREDIT_BET_TYPE_ID = new HashMap<>() {{
        put(CREDIT_NORMAL, "normal");
        put(CREDIT_TIP, "tip");
        put(CREDIT_INSURANCE, "insurance");
        put(CREDIT_DOUBLE, "double");
        put(CREDIT_SPLIT, "split");
        put(CREDIT_SWAP, "swap");
        put(CREDIT_BUY_PLAYER_CARD, "buy player card");
        put(CREDIT_ROYAL_POKER_INSURANCE, "royal poker insurance");
        put(CREDIT_BUY_DEALER_CARD, "buy dealer card");
        put(CREDIT_CALL, "call");
    }};

}
