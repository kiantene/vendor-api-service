package com.nextgen.gameaggregator.vendor.aviatorstudio.constant;

import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.Set;

@Getter
@UtilityClass
public class ReasonCode {
    private static final Set<String> REFUND_REASONS = Set.of(
            "ROUND_CHANGED",
            "PHASE_CHANGED",
            "TIMEOUT_EXCEEDED",
            "REVERSE_FUND",
            "CASHOUT_FAILED",
            "INTEGRATION_TIMED_OUT",
            "INTERNAL_ERROR"
    );

    public static boolean isRefundReason(String reason) {
        return reason != null && REFUND_REASONS.contains(reason);
    }
}
