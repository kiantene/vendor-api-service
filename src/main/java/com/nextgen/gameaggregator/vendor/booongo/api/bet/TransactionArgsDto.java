package com.nextgen.gameaggregator.vendor.booongo.api.bet;

import com.nextgen.gameaggregator.vendor.booongo.dto.PlayerDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigInteger;

@Data
public class TransactionArgsDto {

    private String bet;
    private String win;

    @NotNull
    private Boolean round_started;

    @NotNull
    private Boolean round_finished;

    @NotNull
    private BigInteger round_id;
    private Object bonus;

    @NotNull
    private PlayerDto player;


    private String tag;

    public String getWin() {

        //if win is null then assign 0 value
        if(win == null){
            win = "0";
        }

        return win;
    }

    public void setWin(String win) {
        this.win = win;
    }

    public BigInteger getRound_id() {
        return round_id;
    }

    public void setRound_id(BigInteger round_id) {
        this.round_id = round_id;
    }
}
