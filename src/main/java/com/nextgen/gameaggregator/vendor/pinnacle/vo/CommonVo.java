package com.nextgen.gameaggregator.vendor.pinnacle.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo {
    @JsonProperty("Id")
    private Long id;

    @JsonProperty("TransactionId")
    private Long transactionId;

    @JsonProperty("WagerId")
    private Long wagerId;

    @JsonProperty("ResponseCode")
    private Integer responseCode;
}
