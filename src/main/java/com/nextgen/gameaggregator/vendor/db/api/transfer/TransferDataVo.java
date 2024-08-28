package com.nextgen.gameaggregator.vendor.db.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDataVo {
    private Integer tradeType;
    private BigInteger tradeAmount;
    private BigInteger balance;
}
