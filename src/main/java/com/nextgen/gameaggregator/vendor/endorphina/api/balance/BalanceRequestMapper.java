package com.nextgen.gameaggregator.vendor.endorphina.api.balance;

import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BalanceRequestMapper implements BalanceContextMapper<BalanceRequest> {
    @Override
    public BalanceContext toInternal(BalanceRequest vendorRequest) {
        return BalanceContext.builder()
                .vendorSessionToken(vendorRequest.getToken())
                .build();
    }
}
