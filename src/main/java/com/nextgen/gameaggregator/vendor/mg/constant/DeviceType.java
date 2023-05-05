package com.nextgen.gameaggregator.vendor.mg.constant;

import jakarta.validation.constraints.Pattern;

public enum DeviceType {
    @Pattern(regexp = "^(DESKTOP|TABLET|MOBILE)$")
    DESKTOP,
    TABLET,
    MOBILE
}
