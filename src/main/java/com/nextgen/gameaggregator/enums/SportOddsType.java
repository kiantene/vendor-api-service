package com.nextgen.gameaggregator.enums;

public enum SportOddsType {
    SPECIAL(0),
    MALAY(1),
    CHINA(2),
    DECIMAL(3),
    INDO(4),
    AMERICAN(5),
    EURO(6),
    HONGKONG(7),
    UNKNOWN(999);

    public final int code;

    SportOddsType(int code) {
        this.code = code;
    }
}
