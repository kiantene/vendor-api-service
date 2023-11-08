package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetResultActionsVo {
    @JsonProperty("Id")
    private Long id;

    @JsonProperty("TransactionId")
    private Long transactionId;

    @JsonProperty("WagerId")
    private Long wagerId;

    @JsonProperty("ResponseCode")
    private ResponseCode responseCode;
}
