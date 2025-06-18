package com.nextgen.gameaggregator.vendor.avatarux.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    PLAYER_UNAUTHORIZED("PLAYER_UNAUTHORIZED", "Couldn't authorize the player"),
    SERVER_UNAUTHORIZED("SERVER_UNAUTHORIZED", "Couldn't authorize the server"),
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", "Not enough money to make withdrawal"),
    LOSS_LIMIT("LOSS_LIMIT", "Loss limit has been exceeded"),
    UNKNOWN("UNKNOWN", "Unknown error");

    public final String code;
    public final String description;
}
