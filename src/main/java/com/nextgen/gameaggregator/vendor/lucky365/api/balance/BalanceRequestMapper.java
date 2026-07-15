package com.nextgen.gameaggregator.vendor.lucky365.api.balance;

import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import com.nextgen.gameaggregator.vendor.lucky365.util.LoginIds;
import org.springframework.stereotype.Component;

@Component
public class BalanceRequestMapper implements BalanceContextMapper<BalanceRequest> {
    @Override
    public BalanceContext toInternal(BalanceRequest vendorRequest) {
        return BalanceContext.builder()
                .vendorPlayerUsername(LoginIds.forLookup(vendorRequest.getLoginId()))
                .build();
    }
}
