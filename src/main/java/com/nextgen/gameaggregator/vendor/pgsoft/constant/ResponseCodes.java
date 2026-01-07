package com.nextgen.gameaggregator.vendor.pgsoft.constant;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResponseCodes {

    INVALID_REQUEST("1034", "Invalid request"),
    OPERATION_FAILED("1035", "Operation Failed"),
    INTERNAL_SERVER_ERROR("1200", "Internal server error"),
    INVALID_OPERATOR("1204", "Invalid operator"),
    INVALID_PLAYER_SESSION_1300("1300", "Invalid player session"),
    PLAYER_SESSION_TOKEN_IS_EMPTY("1301", "Player session token is empty"),
    INVALID_PLAYER_SESSION_1302("1302", "Invalid player session"),
    SERVER_ERROR_OCCURS("1303", "Server error occurs"),
    INVALID_PLAYER("1305", "Invalid player"),
    PLAYER_IS_BLOCKED_TO_ACCESS_CURRENT_GAME("1306", "Player is blocked to access current game"),
    INVALID_PLAYER_SESSION_1307("1307", "Invalid player session"),
    PLAYER_SESSION_IS_EXPIRED("1308", "Player session is expired"),
    PLAYER_IS_INACTIVE("1309", "Player is inactive"),
    FAILED_TO_VERIFY_OPERATOR_PLAYER_SESSION("1310", "Failed to verify operator player session"),
    PLAYER_OPERATION_IN_PROGRESS("1315", "Player’s operation in progress"),
    GAME_IS_UNDER_MAINTENANCE("1400", "Game is under maintenance"),
    GAME_IS_INACTIVE("1401", "Game is inactive"),
    GAME_DOES_NOT_EXIST("1402", "Game does not exist or disabled"),
    VALUE_CANNOT_BE_NULL("3001", "Value cannot be null"),
    PLAYER_DOES_NOT_EXIST("3004", "Player does not exist"),
    PLAYER_WALLET_DOES_NOT_EXIST("3005", "Player wallet does not exist"),
    PLAYER_WALLET_ALREADY_EXISTS("3006", "Player wallet already exists"),
    FREE_GAME_DOES_NOT_EXIST("3009", "Free game does not exist"),
    OUT_OF_THE_BALANCE_AMOUNT_TO_TRANSFER_OUT("3013", "Out of the balance amount to transfer out"),
    FREE_GAME_CANNOT_BE_CANCELLED("3014", "Free game cannot be cancelled"),
    NOT_ENOUGH_FREE_GAME("3019", "Not enough free game"),
    NO_BET_EXISTS("3021", "No bet exists"),
    BET_ALREADY_PAY_OUT("3022", "Bet already pay-out"),
    FREE_GAME_EXPIRED("3030", "Free game expired"),
    FREE_GAME_ALREADY_CONVERTED("3031", "Free game already converted"),
    BET_ALREADY_EXISTED("3032", "Bet already existed"),
    BET_FAILED("3033", "Bet failed"),
    PAY_OUT_FAILED("3034", "Pay-out failed"),
    INVALID_MULTIPLIER("3035", "Invalid multiplier"),
    NOT_ENOUGH_BALANCE_TO_CONVERT("3036", "Not enough balance to convert"),
    TRANSACTION_DOES_NOT_EXIST("3040", "Transaction does not exist"),
    NOT_ENOUGH_CASH_BALANCE_TO_BET("3202", "Not enough cash balance to bet"),
    BET_FAILED_3073("3073", "Bet failed"),
    INVALID_REAL_TRANSFER_AMOUNT("3107", "Real transfer amount is invalid");;

    private final String code;
    private final String message;

}