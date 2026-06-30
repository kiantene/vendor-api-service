package com.nextgen.gameaggregator.vendor.groove.api.balance;

import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import com.nextgen.gameaggregator.vendor.groove.util.VendorUtil;
import org.springframework.stereotype.Component;

@Component
public class BalanceRequestMapper implements BalanceContextMapper<BalanceRequest> {
    @Override
    public BalanceContext toInternal(BalanceRequest vendorRequest) {
        return BalanceContext.builder()
                .vendorPlayerUsername(vendorRequest.getAccountid())
                .vendorSessionToken(vendorRequest.getGamesessionid())
                .token(VendorUtil.extractTokenFromSessionId(vendorRequest.getGamesessionid()))
                .build();
    }
}
