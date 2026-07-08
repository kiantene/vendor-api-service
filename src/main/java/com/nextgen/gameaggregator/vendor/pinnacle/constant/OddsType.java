package com.nextgen.gameaggregator.vendor.pinnacle.constant;

import com.nextgen.gameaggregator.enums.SportOddsType;

public enum OddsType {
    AMERICAN(0),
    EURO(1),
    HONGKONG(2),
    INDO(3),
    MALAY(4),
    UNKNOWN(999); // manually create to set unknown

    public final int code;

    OddsType(int code) {
        this.code = code;
    }

    public static int convertToSportOddsCode(int code) {
        return fromCode(code).toSportOddsType().code;
    }

    public static OddsType fromCode(int code) {
        for (OddsType type : OddsType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public SportOddsType toSportOddsType() {
        return switch (this) {
            case AMERICAN -> SportOddsType.AMERICAN;
            case EURO -> SportOddsType.EURO;
            case HONGKONG -> SportOddsType.HONGKONG;
            case INDO -> SportOddsType.INDO;
            case MALAY -> SportOddsType.MALAY;
            default -> SportOddsType.UNKNOWN;
        };
    }
}
