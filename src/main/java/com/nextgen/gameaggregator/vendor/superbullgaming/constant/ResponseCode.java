package com.nextgen.gameaggregator.vendor.superbullgaming.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS                         (0, "SC_OK"),
    OPERATION_FAILED                (-1000, "SC_OPERATION_FAILED"),
    INVALID_REQUEST                 (-1001, "SC_INVALID_REQUEST"),
    INVALID_TOKEN                   (-1101, "SC_INVALID_TOKEN"),
    GAME_DOES_NOT_EXIST             (-1201, "SC_GAME_DOES_NOT_EXIST"),
    INVALID_PLAYER                  (-1301, "SC_INVALID_PLAYER"),
    INACTIVE_PLAYER                 (-1302, "SC_INACTIVE_PLAYER"),
    BET_FAILED                      (-2001, "SC_BET_FAILED"),
    INSUFFICIENT_BALANCE            (-2002, "SC_INSUFFICIENT_BALANCE"),
    PLAYERS_OPERATION_IN_PROGRESS   (-2003, "SC_PLAYERS_OPERATION_IN_PROGRESS"),
    BET_NOT_FOUND                   (-2004, "SC_BET_NOT_FOUND");

    public final Integer code;
    public final String description;
}
