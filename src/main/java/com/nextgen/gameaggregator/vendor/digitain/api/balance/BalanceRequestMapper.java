package com.nextgen.gameaggregator.vendor.digitain.api.balance;

import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BalanceRequestMapper implements BalanceContextMapper<BalanceRequest> {
    @Override
    public BalanceContext toInternal(BalanceRequest vendorRequest) {
        return BalanceContext.builder()
                .vendorPlayerUsername(vendorRequest.getPid())
                .token(vendorRequest.getTkn())
                .vendorSessionToken(vendorRequest.getTkn())
                .build();
    }
}