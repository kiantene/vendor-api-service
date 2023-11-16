package com.nextgen.gameaggregator.vendor.yeebet.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceDto {

    private String appid;

    private String username;

    private String notifyid;

    private String sign;
}
