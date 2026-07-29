package com.nextgen.gameaggregator.vendor.cosmoplay.constant;

import com.nextgen.gameaggregator.vendor.cosmoplay.config.CosmoPlayVendorConfig;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    // --- QA callback: https://qa.gasea168.com/
    // --- Base currently maps to controllers directly
    public static final String PATH = "/api/v1/" + CosmoPlayVendorConfig.CLASS_NAME;

    // --- Actions
    public static final String BETS = "/bet-result";
    public static final String BALANCE = "/auth-check";
    public static final String WIN_RESULT = "/win-result";
    public static final String ROLLBACK = "/cancel-result";
}