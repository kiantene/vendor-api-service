package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoundTxn {
    @JsonProperty("id")
    protected String id;

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

    public static RoundTxn of(GameTransaction txn) {
        RoundTxn roundTxn = new RoundTxn();
        roundTxn.setId(txn.getId());
        roundTxn.setType(txn.getType());
        roundTxn.setGaBetId(txn.getGaBetId());
        roundTxn.setVendorBetId(txn.getVendorBetId());
        roundTxn.setException(txn.getException());
        roundTxn.setStatus(txn.getStatus());
        roundTxn.setSentAt(txn.getSentAt());
        roundTxn.setDoneAt(txn.getDoneAt());

        return roundTxn;
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
    public boolean isSuccessfulBet() {
        return isBet() && isSuccess();
    }

    @JsonIgnore
    public boolean isSuccessfulBetOrResult() {
        return isSuccess() && (isBet() || isResult());
    }
}
