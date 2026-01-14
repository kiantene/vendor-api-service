package com.nextgen.gameaggregator.vendor.lucky365.api.balance;

import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BalanceRequestMapper implements BalanceContextMapper<BalanceRequest> {
    @Override
    public BalanceContext toInternal(BalanceRequest vendorRequest) {
        return BalanceContext.builder()
                .vendorPlayerUsername(vendorRequest.getLoginId().toLowerCase(Locale.ROOT))
                .build();
    }
}
