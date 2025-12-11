package com.nextgen.gameaggregator.core.engine.wallet.result;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BetResultLifeCycleRegistry {
    
    private final Map<String, BetResultLifeCycle> handlerMap;

    public BetResultLifeCycleRegistry(List<BetResultLifeCycle> handlers) {
        this.handlerMap = handlers.stream()
            .collect(Collectors.toMap(BetResultLifeCycle::getVendorClassName, Function.identity()));
    }

    public BetResultLifeCycle getHandler(String vendorClassName) {
        return handlerMap.get(vendorClassName);
    }
}
