package com.nextgen.gameaggregator.vendor.egtdigital.api.bet;

import com.nextgen.gameaggregator.vendor.egtdigital.vo.ResponseCommonVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
public class BetResponse extends ResponseCommonVo {

    private String casinoTransferId;

    private Long bonusAmount;

    private Long realAmount;
}
