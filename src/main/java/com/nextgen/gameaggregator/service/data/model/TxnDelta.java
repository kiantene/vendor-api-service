package com.nextgen.gameaggregator.service.data.model;

import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;

import java.math.BigDecimal;
import java.util.Optional;

public record TxnDelta(
        String docId,                 // game round doc id
        int idx,                      // transactions[idx]
        TxnType txnType,
        String gaBetId,
        Optional<BigDecimal> lastBalance,
        Optional<BigDecimal> betDelta,// +bet on SUCCESS, empty otherwise
        Optional<BigDecimal> winDelta,// +win on SUCCESS, empty otherwise
        Optional<BigDecimal> jackpotDelta,// +jackpot on SUCCESS, empty otherwise
        Optional<BigDecimal> effectiveTurnover, // replaces (not accumulates) when present
        Optional<TxnStatus> status,   // SENT / SUCCESS / FAILED (when changing status)
        Optional<TimeField> timeField,// which time to write
        Optional<String> timeValueUtc,// "HH:mm:ss.SSS" in UTC
        boolean isSettled,            // if true, set state=SETTLED and apply TTL'
        boolean isEnded
) {

    public static TxnDelta finalizeSuccess(String docId,
                                           int idx,
                                           TxnType txnType,
                                           String gaBetId,
                                           BigDecimal lastBalance,
                                           BigDecimal betDelta,
                                           BigDecimal winDelta,
                                           BigDecimal jackpotDelta,
                                           BigDecimal effectiveTurnover,
                                           String doneAtUtc,
                                           boolean isSettled,
                                           boolean isEnded) {
        return new TxnDelta(
                docId,
                idx,
                txnType,
                gaBetId,
                Optional.ofNullable(lastBalance),
                Optional.ofNullable(betDelta),
                Optional.ofNullable(winDelta),
                Optional.ofNullable(jackpotDelta),
                Optional.ofNullable(effectiveTurnover),
                Optional.of(TxnStatus.SUCCESS),
                Optional.of(TimeField.DONE_AT),
                Optional.of(doneAtUtc),
                isSettled,
                isEnded
        );
    }

    public enum TimeField { SENT_AT, DONE_AT }
}
