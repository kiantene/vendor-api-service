package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoundTxn {
    @JsonProperty("id")
    protected String id;

    @JsonProperty("state")
    protected GameRoundState state;

    @JsonProperty("type")
    protected TxnType type;

    @JsonProperty("gaBetId")
    protected String gaBetId;

    @JsonProperty("vendorBetId")
    protected String vendorBetId;

    @JsonProperty("status")
    protected TxnStatus status;

    @JsonProperty("exception")
    protected String exception;

    @JsonProperty("sentAt")
    protected String sentAt;

    @JsonProperty("doneAt")
    protected String doneAt;

    @JsonProperty("betAmount")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    protected BigDecimal betAmount;

    @JsonProperty("winAmount")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    protected BigDecimal winAmount;

    @JsonProperty("jackpotAmount")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    protected BigDecimal jackpotAmount;

    /**
     * Capped (agent max-payout applied) counterparts of {@link #winAmount}/{@link #jackpotAmount},
     * in the same vendor units. Null when this txn was never capped (no cap config, within cap, or
     * a non-WIN / pre-feature txn) — read via {@link #cappedWinAmountOrVendor()} /
     * {@link #cappedJackpotAmountOrVendor()} which fall back to the uncapped vendor amount.
     */
    @JsonProperty("cappedWinAmount")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    protected BigDecimal cappedWinAmount;

    @JsonProperty("cappedJackpotAmount")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    protected BigDecimal cappedJackpotAmount;

    public RoundTxn() {
        this.state = GameRoundState.UNSETTLED;
    }

    public static RoundTxn of(GameTransaction txn) {
        RoundTxn roundTxn = new RoundTxn();
        roundTxn.setId(txn.getId());
        roundTxn.setState(txn.getState());
        roundTxn.setType(txn.getType());
        roundTxn.setGaBetId(txn.getGaBetId());
        roundTxn.setVendorBetId(txn.getVendorBetId());
        roundTxn.setWinAmount(txn.getWinAmount());
        roundTxn.setJackpotAmount(txn.getJackpotAmount());
        roundTxn.setException(txn.getException());
        roundTxn.setStatus(txn.getStatus());
        roundTxn.setSentAt(txn.getSentAt());
        roundTxn.setDoneAt(txn.getDoneAt());
        roundTxn.setBetAmount(txn.getBetAmount());
        roundTxn.setWinAmount(txn.getWinAmount());
        roundTxn.setJackpotAmount(txn.getJackpotAmount());
        // Secondary append-time copy (e.g. BET_N_RESULT already carries the cap). For the
        // SettleByRound WIN result the cap runs AFTER the slice is appended, so the
        // authoritative persistence is TxnDelta.finalizeSuccess -> applyTxnDelta.
        roundTxn.setCappedWinAmount(txn.getCappedWinAmount());
        roundTxn.setCappedJackpotAmount(txn.getCappedJackpotAmount());

        return roundTxn;
    }

    /** Capped win, falling back to the uncapped vendor amount when no cap was recorded. */
    @JsonIgnore
    public BigDecimal cappedWinAmountOrVendor() {
        return cappedWinAmount != null ? cappedWinAmount : winAmount;
    }

    /** Capped jackpot, falling back to the uncapped vendor amount when no cap was recorded. */
    @JsonIgnore
    public BigDecimal cappedJackpotAmountOrVendor() {
        return cappedJackpotAmount != null ? cappedJackpotAmount : jackpotAmount;
    }

    @JsonIgnore
    public String getRollbackId(String className) {
        return className + "::" + TxnType.BET + "::" + vendorBetId;
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
    public boolean isError() {
        return status == TxnStatus.ERROR;
    }

    @JsonIgnore
    public boolean isStillProcessing() {
        return status == TxnStatus.SENT;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return status == TxnStatus.SUCCESS;
    }

    @JsonIgnore
    public boolean isBet() {
        return type == TxnType.BET;
    }

    @JsonIgnore
    public boolean isResult() {
        return type == TxnType.RESULT;
    }

    @JsonIgnore
    public boolean isBetNResult() {
        return type == TxnType.BET_N_RESULT;
    }

    @JsonIgnore
    public boolean isRollback() {
        return type == TxnType.ROLLBACK;
    }

    @JsonIgnore
    public boolean isSuccessfulBet() {
        return isBet() && isSuccess();
    }

    @JsonIgnore
    public boolean isSuccessfulResult() {
        return isResult() && isSuccess();
    }

    @JsonIgnore
    public boolean isSuccessfulBetOrResult() {
        return isSuccess() && (isBet() || isResult() || isBetNResult());
    }

    @JsonIgnore
    public boolean hasAliasTxn(String className) {
        return !id.equals(getRollbackId(className));
    }
}
