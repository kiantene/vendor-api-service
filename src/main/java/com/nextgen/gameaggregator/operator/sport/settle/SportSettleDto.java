package com.nextgen.gameaggregator.operator.sport.settle;

import com.nextgen.gameaggregator.operator.sport.bet.SportBetDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class SportSettleDto extends SportBetDto {
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
}
