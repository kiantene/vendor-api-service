package com.nextgen.gameaggregator.vendor.bng.api.bet;

import lombok.Data;

import java.math.BigInteger;

@Data
public class TransactionArgsDto {
    private String bet;
    private String win;
    private Boolean round_started;
    private Boolean round_finished;
    private BigInteger round_id;
    private Object player;
    private Object bonus;
    private String tag;

    public BigInteger getRound_id() {
        return round_id;
    }

    public void setRound_id(BigInteger round_id) {
        this.round_id = round_id;
    }
}
