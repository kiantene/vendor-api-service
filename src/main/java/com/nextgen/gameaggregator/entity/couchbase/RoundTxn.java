package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.TxnStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoundTxn {
    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private TxnStatus status;

    @JsonProperty("sentAt")
    private String sentAt;

    @JsonProperty("doneAt")
    private String doneAt;

    public RoundTxn(String id) {
        this.id = id;
    }

    public RoundTxn(String id, TxnStatus status) {
        this.id = id;
        this.status = status;
    }
}
