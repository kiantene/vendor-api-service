package com.nextgen.gameaggregator.vendor.joker.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceDto {

    private String username;

    private Long timestamp;

    private String appid;

    private String hash;
}
