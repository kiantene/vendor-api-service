package com.nextgen.gameaggregator.vendor.jdb.api.balance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BalanceDto {
    private String action;
    private Long ts;
    private String uid;
    private String currency;
    @JsonProperty("gType")
    private Integer gType;
}
