package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class EvoplayFreeRoundPayoutResponseMapper implements PromoPayoutVendorResponseMapper<EvoplayFreeRoundPayoutResult> {

    @Override
    public EvoplayFreeRoundPayoutResult toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        BigDecimal balance = Optional.ofNullable(balanceData)
                .map(PlayerBalanceData::getBalance)
                .orElse(BigDecimal.ZERO);
        String currency = Optional.ofNullable(balanceData)
                .map(PlayerBalanceData::getCurrency)
                .filter(value -> !value.isBlank())
                .orElse(context.getVendorCurrency());

        return EvoplayFreeRoundPayoutResult.builder()
                .balance(balance)
                .currency(currency)
                .build();
    }
}
