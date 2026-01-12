package com.nextgen.gameaggregator.vendor.spribe.api.v2.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.spribe.response.BalanceResponse;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.springframework.stereotype.Component;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<BalanceResponse> {
    @Override
    public BalanceResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        BalanceResponse.Data data = BalanceResponse.Data.builder()
                .userId(balanceData.getUsername())
                .username(balanceData.getUsername())
                .currency(balanceData.getCurrency())
                .balance(AmountConverter.convertBalanceToUnit(balanceData.getBalance()))
                .build();

        return new BalanceResponse(data);
    }
}
