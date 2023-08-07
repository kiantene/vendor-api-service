package com.nextgen.gameaggregator.vendor.bgaming.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionVo {
    @JsonProperty("action_id")
    private String actionId;
    @JsonProperty("tx_id")
    private String txId;
    @JsonProperty("processed_at")
    private String processedAt;
    private Integer balance;
}
