package com.nextgen.gameaggregator.sport.operator.wallet.settle;

import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportWalletSettleDto extends WalletBetResultDto {
    private BigDecimal actualBetAmount;
    private BigDecimal odds;
    private Integer oddsType;
}
