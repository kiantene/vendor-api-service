package com.nextgen.gameaggregator.vendor.iloveu.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum GameType {

    SETTLE (3, "Settle"),
    BETNSETTLE (4, "Bet and Settle")
    ;

    public final Integer code;
    public final String description;

}
