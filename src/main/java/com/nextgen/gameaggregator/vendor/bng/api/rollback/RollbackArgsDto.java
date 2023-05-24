package com.nextgen.gameaggregator.vendor.bng.api.rollback;

import lombok.Data;
import java.math.BigInteger;

@Data
public class RollbackArgsDto {
    private String transaction_uid;
    private String bet;
    private String win;
    private Boolean round_started;
    private Boolean round_finished;
    private BigInteger round_id;
    private Object bonus;
    private Object player;
    private String tag;
}
