package com.nextgen.gameaggregator.vendor.lucky365.constant;

public enum Mode {
    BET_RESULT(3),
    BET_AND_RESULT(4);

    private final int code;

    Mode(int code) {
        this.code = code;
    }

    public static Mode fromCode(Integer code) {
        for (Mode m : values()) {
            if (m.code == code) {
                return m;
            }
        }
        throw new IllegalArgumentException("Invalid mode: " + code);
    }
}