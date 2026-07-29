package com.nextgen.gameaggregator.vendor.evoplay.api.v2.balance;

import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import org.springframework.stereotype.Component;

@Component
public class BalanceRequestMapper implements BalanceContextMapper<CallbackDto> {
    @Override
    public BalanceContext toInternal(CallbackDto request) {
        return BalanceContext.builder()
                .token(request.getToken())
                .vendorPlayerUsername(request.getUsername())
                .build();
    }
}
