package com.nextgen.gameaggregator.vendor.gpkv2.api.balance;

import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContextMapper;
import com.nextgen.gameaggregator.vendor.gpkv2.api.dto.CommonDto;
import org.springframework.stereotype.Component;

@Component
public class BalanceRequestMapper implements BalanceContextMapper<CommonDto> {
    @Override
    public BalanceContext toInternal(CommonDto vendorRequest) {
        return BalanceContext.builder()
                .token(vendorRequest.getSessionToken())
                .build();
    }
}
