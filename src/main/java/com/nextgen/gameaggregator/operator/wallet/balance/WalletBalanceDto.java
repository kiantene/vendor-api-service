package com.nextgen.gameaggregator.operator.wallet.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletBalanceDto {
    private String traceId;
    private String username;
    private String currency;
    private String token;
    private String gameCode;
}
