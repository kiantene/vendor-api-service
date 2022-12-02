package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.bet;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.vo.AbstractActionVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetActionVo extends AbstractActionVo {
    private String transactionId;
    private String currency;
    private BigDecimal cash;
    private BigDecimal bonus;
    private BigDecimal usedPromo;
}
