package com.nextgen.gameaggregator.core.engine;

import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerBalanceData {
    private String username;
    private String currency;
    private BigDecimal balance;
    private Long timestamp;

    public static PlayerBalanceData getDefault(String username, String currency) {
        currency = Optional.ofNullable(currency).orElse("");

        return new PlayerBalanceData(
                username,
                currency,
                BigDecimal.ZERO,
                System.currentTimeMillis()
        );
    }

    public PlayerBalanceData toVendorView(String username, String currency, BigDecimal toVendorRate) {
        return new PlayerBalanceData(
                username,
                currency,
                TxnAmount.of(balance, toVendorRate).amount(),
                timestamp
        );
    }
}
