package com.nextgen.gameaggregator.core.engine.wallet.bet;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BetLifeCycleRegistry {
    
    private final Map<String, BetLifeCycle> handlerMap;

    public BetLifeCycleRegistry(List<BetLifeCycle> handlers) {
        this.handlerMap = handlers.stream()
            .collect(Collectors.toMap(BetLifeCycle::getVendorClassName, Function.identity()));
    }

    public BetLifeCycle getHandler(String vendorClassName) {
        return handlerMap.get(vendorClassName);
    }
}
