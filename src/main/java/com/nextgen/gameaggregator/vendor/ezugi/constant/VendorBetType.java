package com.nextgen.gameaggregator.vendor.ezugi.constant;

import com.nextgen.gameaggregator.enums.BetType;

import java.util.HashMap;
import java.util.Map;

public enum VendorBetType {
    REGULAR_BET             ("RegularBet", BetType.NORMAL_BET),
    TWENTY_ONE_PLUS_THREE   ("21+3", BetType.TWENTY_ONE_PLUS_THREE),
    PERFECT_PAIR            ("PerfectPair", BetType.PERFECT_PAIR),
    LUCKY_LADIES            ("LuckyLadies", BetType.LUCKY_LADIES),
    TEN_20                  ("Ten20", BetType.TEN_20),
    PERFECT_11              ("Perfect11", BetType.PERFECT_11),
    PLAYER_BET              ("PlayerBet", BetType.NORMAL_BET),
    RAZZ_BONUS              ("RazzBonus", BetType.RAZZ_BONUS)
    ;

    private final String vendorValue;
    private final BetType internalBetType;
    private static final Map<String, VendorBetType> VENDOR_VALUE_MAP = new HashMap<>();

    static {
        for (VendorBetType type : values()) {
            VENDOR_VALUE_MAP.put(type.vendorValue, type);
        }
    }

    VendorBetType(String vendorValue, BetType internalBetType) {
        this.vendorValue = vendorValue;
        this.internalBetType = internalBetType;
    }

    public String getVendorValue() {
        return vendorValue;
    }

    public BetType getInternalBetType() {
        return internalBetType;
    }

    public static VendorBetType fromVendorValue(String vendorValue) {
        if (vendorValue == null) {
            throw new IllegalArgumentException("Vendor bet type cannot be null");
        }

        VendorBetType type = VENDOR_VALUE_MAP.get(vendorValue);
        if (type == null) {
            throw new IllegalArgumentException("Unknown vendor bet type: " + vendorValue);
        }

        return type;
    }

    public static BetType toInternalBetType(String vendorValue) {
        if (vendorValue == null) {
            return BetType.NORMAL_BET;
        }

        VendorBetType vendorType = VENDOR_VALUE_MAP.get(vendorValue);
        return vendorType != null ? vendorType.getInternalBetType() : BetType.NORMAL_BET;
    }

    @Override
    public String toString() {
        return vendorValue;
    }
}
