package com.nextgen.gameaggregator.vendor.db.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceDataVo {
    private BigInteger balance;
}
