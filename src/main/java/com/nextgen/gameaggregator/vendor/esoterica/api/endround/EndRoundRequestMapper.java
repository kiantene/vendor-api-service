package com.nextgen.gameaggregator.vendor.esoterica.api.endround;

import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import org.springframework.stereotype.Component;

@Component
public class EndRoundRequestMapper implements BalanceContextMapper<EndRoundRequest> {
    @Override
    public BalanceContext toInternal(EndRoundRequest request) {
        return BalanceContext.builder()
                .vendorPlayerUsername(request.getUserId())
                .build();
    }
}
