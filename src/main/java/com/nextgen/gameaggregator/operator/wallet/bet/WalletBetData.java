package com.nextgen.gameaggregator.operator.wallet.bet;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletBetData {
    private String username;
    private String currency;
    private BigDecimal balance;
}
