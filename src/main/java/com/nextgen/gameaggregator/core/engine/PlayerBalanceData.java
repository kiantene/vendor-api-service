package com.nextgen.gameaggregator.core.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerBalanceData {
    private String username;
    private String currency;
    private BigDecimal balance;
    private Long timestamp;

    public static PlayerBalanceData getDefault(String username, String currency) {
        return new PlayerBalanceData(
                username,
                currency,
                BigDecimal.ZERO,
                System.currentTimeMillis()
        );
    }
}
