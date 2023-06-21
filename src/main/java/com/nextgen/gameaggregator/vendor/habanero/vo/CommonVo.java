package com.nextgen.gameaggregator.vendor.habanero.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo{

    @JsonProperty("accountid")
    private String accountId;

    @JsonProperty("accountname")
    private String accountnName;

    @JsonProperty("balance")
    private String balance;

    @JsonProperty("currencycode")
    private String currencyCode;
}
