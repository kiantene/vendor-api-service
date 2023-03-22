package com.nextgen.gameaggregator.vendor.jdb.api.balance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class BalanceDto {
    private Integer action;
    @Size(min = 13, max = 13)
    private Long ts;
    @Size(min = 13, max = 13)
    private String uid;
    private String currency;
    @JsonProperty("gType")
    private Integer gType;
}
