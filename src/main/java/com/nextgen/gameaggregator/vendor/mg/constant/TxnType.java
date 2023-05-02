package com.nextgen.gameaggregator.vendor.mg.constant;

import jakarta.validation.constraints.Pattern;

public enum TxnType {
    @Pattern(regexp = "^(DEBIT|CREDIT)$")
    DEBIT,
    CREDIT
}
