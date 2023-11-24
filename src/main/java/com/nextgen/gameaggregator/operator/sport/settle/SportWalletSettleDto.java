package com.nextgen.gameaggregator.operator.sport.settle;

import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportWalletSettleDto extends WalletBetResultDto {
    private BigDecimal actualBetAmount;
    private BigDecimal odds;
    private Integer oddsType;
}
