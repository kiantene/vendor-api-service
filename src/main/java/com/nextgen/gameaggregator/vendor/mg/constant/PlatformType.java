package com.nextgen.gameaggregator.vendor.mg.constant;

import jakarta.validation.constraints.Pattern;

public enum PlatformType {
    @Pattern(regexp = "^(H5|NATIVE)$")
    H5,
    NATIVE
}
