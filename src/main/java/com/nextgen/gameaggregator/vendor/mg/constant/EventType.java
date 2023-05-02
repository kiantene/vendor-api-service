package com.nextgen.gameaggregator.vendor.mg.constant;

import jakarta.validation.constraints.Pattern;

public enum EventType {
    @Pattern(regexp = "^(GAME|TOURNAMENT|PROMOTION|ACHIEVENMENT|STORE)$")
    GAME,
    TOURNAMENT,
    PROMOTION,
    ACHIEVENMENT,
    STORE
}
