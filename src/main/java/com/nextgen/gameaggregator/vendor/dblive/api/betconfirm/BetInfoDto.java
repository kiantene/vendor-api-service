package com.nextgen.gameaggregator.vendor.dblive.api.betconfirm;

import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
public class BetInfoDto {
    private BigDecimal betAmount;
    private int betPointId;
    private BigInteger betId;
}
