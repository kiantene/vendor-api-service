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
    private String id;

    @JsonProperty("type")
    private TxnType type;

    @JsonProperty("gaBetId")
    private String gaBetId;

    @JsonProperty("status")
    private TxnStatus status;

    @JsonProperty("exception")
    private String exception;

    @JsonProperty("sentAt")
    private String sentAt;

    @JsonProperty("doneAt")
    private String doneAt;

    public static RoundTxn of(GameTransaction txn) {
        RoundTxn roundTxn = new RoundTxn();
        roundTxn.setId(txn.getId());
        roundTxn.setType(txn.getType());
        roundTxn.setStatus(txn.getStatus());
        roundTxn.setSentAt(txn.getSentAt());
        roundTxn.setDoneAt(txn.getDoneAt());

        return roundTxn;
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
    public boolean isSuccessfulBet() {
        return isBet() && isSuccess();
    }
}
