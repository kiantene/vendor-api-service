package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetResultActionsVo {
    private Long Id;
    private Long TransactionId;
    private Long WagerId;
    private Integer ResponseCode;
}
