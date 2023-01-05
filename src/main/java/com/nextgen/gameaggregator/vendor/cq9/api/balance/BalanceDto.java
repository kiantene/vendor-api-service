package com.nextgen.gameaggregator.vendor.cq9.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    public String gamecode;
}
