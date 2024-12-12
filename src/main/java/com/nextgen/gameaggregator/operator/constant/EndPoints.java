package com.nextgen.gameaggregator.operator.constant;

public class EndPoints {

    public static final Integer TIMEOUT = 5000;
    public static final Integer SPORTBOOK_TIMEOUT = 5000;
    public static final Integer RETRY_COUNT = 3;
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String GAME_URL = "/game/url";
    public static final String WALLET_BALANCE = "/wallet/balance";
    public static final String WALLET_BET = "/wallet/bet";
    public static final String WALLET_BET_RESULT = "/wallet/bet_result";
    public static final String WALLET_WIN = "/wallet/win";
    public static final String WALLET_ROLLBACK = "/wallet/rollback";
    public static final String WALLET_BET_DEBIT = "/wallet/bet_debit";
    public static final String WALLET_BET_CREDIT = "/wallet/bet_credit";

    public static final String WALLET_CREATE_BET_RESULT = "/wallet/create_bet_result";

    public static final String API_VERIFY_PATH = "/api_verify";

    public static final String GET_VERIFY_INFO = "/get_verify_info";

    public static final String CREATE_VERIFY_REPORT = "/create_verify_report";
    public static final String UPDATE_VERIFY_TEST_CASE = "/update_verify_test_case";

    public static final String WALLET_ADJUSTMENT = "/wallet/adjustment";

    // SportBook
    public static final String SPORT_BET = "/sports/bet";
    public static final String SPORT_UPDATE_BET = "/sports/update-bet";
    public static final String SPORT_SETTLE = "/sports/settled";
    public static final String SPORT_CANCEL_BET = "/sports/cancel-bet";
    public static final String SPORT_UNSETTLE = "/sports/unsettle";
    public static final String SPORT_REFUND = "/sports/refund";
    public static final String SPORT_RESETTLE = "/sports/resettle";
    public static final String SPORT_ADJUSTMENT = "/sports/adjustment";
}
