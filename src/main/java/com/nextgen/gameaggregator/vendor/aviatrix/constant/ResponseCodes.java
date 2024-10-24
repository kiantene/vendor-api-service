package com.nextgen.gameaggregator.vendor.aviatrix.constant;

public class ResponseCodes {
    //status 400
    public static final String INVALID_SESSION_TOKEN = "Invalid session token";
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String INVALID_TRANSACTION = "Invalid transaction";
    public static final String INVALID_PLAYER_CURRENCY = "Invalid player currency";
    //status 401
    public static final String SESSION_TOKEN_EXPIRED = "Session token expired";
    //status 403
    public static final String INSUFICIENT_BALANCE = "Insufficient balance";
    public static final String PLAYER_BANNED = "Player banned";
    //status 404
    public static final String PRODUCT_NOT_FOUND = "Product not found";
    public static final String PLAYER_NOT_FOUND = "Player not found";
    public static final String BET_NOT_FOUND = "Bet not found";
    public static final String PLATFORM_NOT_FOUND = "Platform not found";
    //status 500
    public static final String UNKNOWN_ERROR = "Unknown error";

    //only need return message with specific status code
    private ResponseCodes() {
    }


}
