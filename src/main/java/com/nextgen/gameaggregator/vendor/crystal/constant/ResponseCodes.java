package com.nextgen.gameaggregator.vendor.crystal.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {

    INVALID_PARAMETERS(400, "Request parameters are not valid"),
    INVALID_SIGNATURE(401, "Signature is not valid"),
    INSUFFICIENT_FUNDS(402, "Insufficient funds."),
    TRANSACTION_NOT_FOUND(403, "Transaction not found."),
    PLAYER_NOT_FOUND(404, "Player not found.");

    public final Integer code;
    public final String message;
}
