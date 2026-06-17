package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nextgen.gameaggregator.enums.GameRoundState;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.requireNonNullElse;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameRound {

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("state")
    private GameRoundState state;

    @JsonProperty("className")
    private String className;

    @JsonProperty("vendorId")
    private Integer vendorId;

    @JsonProperty("roundId")
    private String roundId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("gameCode")
    private String gameCode;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("lastBalance")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal lastBalance;

    // Legacy stored aggregates. No longer written by applyTxnDelta — kept as
    // private fields purely so Jackson can deserialize legacy JSON without
    // failing. Lombok's auto-getter is suppressed (see @Getter(AccessLevel.NONE))
    // and the manual getters below delegate to computeTotals() so any straggling
    // caller of round.getBetAmount() etc. receives the live derived total.
    @JsonProperty("betAmount")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    @Getter(AccessLevel.NONE)
    private BigDecimal betAmount;

    @JsonProperty("winAmount")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    @Getter(AccessLevel.NONE)
    private BigDecimal winAmount;

    @JsonProperty("jackpotAmount")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    @Getter(AccessLevel.NONE)
    private BigDecimal jackpotAmount;

    @JsonProperty("effectiveTurnover")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal effectiveTurnover;

    @JsonProperty("agentMeta")
    private AgentMeta agentMeta;

    @JsonProperty("txnCount")
    private Integer txnCount;

    @JsonProperty("betTxnCount")
    private Integer betTxnCount;

    @JsonProperty("transactions")
    private List<RoundTxn> transactions;

    @JsonProperty("isEnded")
    private Boolean isEnded;

    @JsonProperty("createdTs")
    private Long createdTs;

    public GameRound() {
        this.txnCount = 1;
        this.betTxnCount = 0;
        this.state = GameRoundState.UNSETTLED;
        this.betAmount = BigDecimal.ZERO;
        this.winAmount = BigDecimal.ZERO;
        this.jackpotAmount = BigDecimal.ZERO;
        this.isEnded = false;
    }

    public GameRound(String className, String username, String roundId) {
        this();
        this.className = className;
        this.username = username;
        this.username = username;
        this.roundId = roundId;
    }

    public static GameRound of(String className, String username, String roundId) {
        return new GameRound(className, username, roundId);
    }

    @JsonIgnore
    public String getId() {
        return className + "::" + username + "::" + roundId;
    }

    @JsonIgnore
    public boolean isSettled() {
        return state == GameRoundState.SETTLED;
    }

    @JsonIgnore
    public boolean isUnsettled() {
        return state == GameRoundState.UNSETTLED;
    }

    @JsonIgnore
    public boolean isRefunded() {
        return state == GameRoundState.REFUNDED;
    }

    @JsonIgnore
    public boolean isVoid() {
        return state == GameRoundState.VOID;
    }

    @JsonIgnore
    public boolean hasMultipleBets() {
        return betTxnCount != null && betTxnCount >= 1;
    }

    @JsonIgnore
    public boolean isEnded() {
        return Boolean.TRUE.equals(isEnded);
    }

    @JsonIgnore
    public BigDecimal getLastBalanceWithDefault() {
        return Optional.ofNullable(lastBalance).orElse(BigDecimal.ZERO);
    }

    public void setBetAmount(BigDecimal betAmount) {
        this.betAmount = (betAmount == null) ? BigDecimal.ZERO : betAmount;
    }

    public void setWinAmount(BigDecimal winAmount) {
        this.winAmount = (winAmount == null) ? BigDecimal.ZERO : winAmount;
    }

    public void setTransactions(List<RoundTxn> transactions) {
        if (transactions == null) return;

        this.transactions = transactions;
        this.txnCount = transactions.size();
    }

    /**
     * Per-slot amount aggregates derived from transactions[]. Replaces the
     * previous read-modify-write on the round-level betAmount/winAmount/
     * jackpotAmount, which contended under concurrent settles.
     */
    public record Totals(BigDecimal bet, BigDecimal win, BigDecimal jackpot) {}

    /**
     * @deprecated The stored round-level betAmount is no longer maintained.
     * Use {@link #computeTotals()} for correctness on new rounds.
     */
    @Deprecated
    @JsonIgnore
    public BigDecimal getBetAmount() { return computeTotals().bet(); }

    /**
     * @deprecated The stored round-level winAmount is no longer maintained.
     * Use {@link #computeTotals()} for correctness on new rounds.
     */
    @Deprecated
    @JsonIgnore
    public BigDecimal getWinAmount() { return computeTotals().win(); }

    /**
     * @deprecated The stored round-level jackpotAmount is no longer maintained.
     * Use {@link #computeTotals()} for correctness on new rounds.
     */
    @Deprecated
    @JsonIgnore
    public BigDecimal getJackpotAmount() { return computeTotals().jackpot(); }

    @JsonIgnore
    public Totals computeTotals() {
        BigDecimal bet = ZERO, win = ZERO, jackpot = ZERO;
        if (transactions != null) {
            for (RoundTxn t : transactions) {
                if (!t.isSuccess()) continue;
                switch (t.getType()) {
                    case BET -> {
                        // A refunded BET has been cancelled by a ROLLBACK — exclude from totals.
                        if (!t.isRefunded()) {
                            bet = bet.add(requireNonNullElse(t.getBetAmount(), ZERO));
                        }
                    }
                    case RESULT -> {
                        win     = win.add(requireNonNullElse(t.getWinAmount(),     ZERO));
                        jackpot = jackpot.add(requireNonNullElse(t.getJackpotAmount(), ZERO));
                    }
                    case BET_N_RESULT -> {
                        bet     = bet.add(requireNonNullElse(t.getBetAmount(),     ZERO));
                        win     = win.add(requireNonNullElse(t.getWinAmount(),     ZERO));
                        jackpot = jackpot.add(requireNonNullElse(t.getJackpotAmount(), ZERO));
                    }
                    // ROLLBACK is a no-op here — the BET it targets is already marked REFUNDED
                    // (markRefunded runs before the rollback reaches SUCCESS) and excluded above.
                    // DEBIT, CREDIT, PAYOUT are wallet-level ops — not part of bet/win/jackpot.
                    case ROLLBACK, DEBIT, CREDIT, PAYOUT -> {}
                }
            }
        }
        return new Totals(bet, win, jackpot);
    }
}
