package com.nextgen.gameaggregator.vendor.saba.constant;

import com.nextgen.gameaggregator.enums.SportOddsType;

public enum OddsType {
    SPECIAL(0),
    MALAY(1),
    CHINA(2),
    DECIMAL(3),
    INDO(4),
    AMERICAN(5),
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
            case SPECIAL -> SportOddsType.SPECIAL;
            case MALAY -> SportOddsType.MALAY;
            case CHINA -> SportOddsType.CHINA;
            case DECIMAL -> SportOddsType.DECIMAL;
            case INDO -> SportOddsType.INDO;
            case AMERICAN -> SportOddsType.AMERICAN;
            default -> SportOddsType.UNKNOWN;
        };
    }
}
