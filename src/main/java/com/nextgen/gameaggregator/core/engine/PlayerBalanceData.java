package com.nextgen.gameaggregator.core.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@Data
@NoArgsConstructor
public class PlayerBalanceData {
    private String username;
    private String currency;
    private BigDecimal balance;
    private Long timestamp;

    /** True when this instance already holds a vendor-view balance and must not have toVendorRate applied again. */
    @JsonIgnore
    private boolean alreadyInVendorView;

    public PlayerBalanceData(String username, String currency, BigDecimal balance, Long timestamp) {
        this.username = username;
        this.currency = currency;
        this.balance = balance;
        this.timestamp = timestamp;
        this.alreadyInVendorView = false;
    }

    public static PlayerBalanceData getDefault(String username, String currency) {
        currency = Optional.ofNullable(currency).orElse("");

        return new PlayerBalanceData(
                username,
                currency,
                BigDecimal.ZERO,
                System.currentTimeMillis()
        );
    }

    public static PlayerBalanceData getDefaultWithBalance(String username, String currency, BigDecimal balance) {
        currency = Optional.ofNullable(currency).orElse("");

        return new PlayerBalanceData(
                username,
                currency,
                balance,
                System.currentTimeMillis()
        );
    }

    /**
     * Use when the balance is already in vendor view (e.g. a fallback from a persisted last-known balance).
     * Calling {@link #toVendorView} on the returned instance will be a no-op on the rate conversion.
     */
    public static PlayerBalanceData ofVendorView(String username, String currency, BigDecimal balance) {
        currency = Optional.ofNullable(currency).orElse("");
        PlayerBalanceData data = new PlayerBalanceData(username, currency, balance, System.currentTimeMillis());
        data.alreadyInVendorView = true;
        return data;
    }

    /**
     * Converts this balance to vendor view by applying {@code toVendorRate}.
     * If the balance is already in vendor view (see {@link #ofVendorView}), the rate multiplication is skipped.
     */
    public PlayerBalanceData toVendorView(String username, String currency, BigDecimal toVendorRate) {
        BigDecimal converted = alreadyInVendorView
                ? balance
                : TxnAmount.of(balance, toVendorRate).amount();
        return new PlayerBalanceData(username, currency, converted, timestamp);
    }
}
