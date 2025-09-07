package com.nextgen.gameaggregator.service.data.model;

import com.nextgen.gameaggregator.enums.TxnStatus;

import java.math.BigDecimal;
import java.util.Optional;

public record TxnDelta(
        String docId,                 // game round doc id
        int idx,                      // transactions[idx]
        String gaBetId,
        Optional<BigDecimal> betDelta,// +bet on SUCCESS, empty otherwise
        Optional<BigDecimal> winDelta,// +win on SUCCESS, empty otherwise
        Optional<TxnStatus> status,   // SENT / SUCCESS / FAILED (when changing status)
        Optional<TimeField> timeField,// which time to write
        Optional<String> timeValueUtc,// "HH:mm:ss.SSS" in UTC
        boolean isSettled             // if true, set state=SETTLED and apply TTL
) {

    public static TxnDelta finalizeSuccess(String docId,
                                           int idx,
                                           String gaBetId,
                                           BigDecimal betDelta,
                                           BigDecimal winDelta,
                                           String doneAtUtc,
                                           boolean isSettled) {
        return new TxnDelta(
                docId,
                idx,
                gaBetId,
                Optional.ofNullable(betDelta),
                Optional.ofNullable(winDelta),
                Optional.of(TxnStatus.SUCCESS),
                Optional.of(TimeField.DONE_AT),
                Optional.of(doneAtUtc),
                isSettled
        );
    }

    public enum TimeField { SENT_AT, DONE_AT }
}
