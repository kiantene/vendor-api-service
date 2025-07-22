package com.nextgen.gameaggregator.core.engine.game.url;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GameLauncherRegistry {
    private final Map<String, AbstractGameLaunchHandler<?, ?>> handlerMap;

    public GameLauncherRegistry(List<AbstractGameLaunchHandler<?, ?>> handlers) {
        this.handlerMap = handlers.stream()
                .filter(handler -> handler.getVendorClassName() != null)
                .collect(Collectors.toMap(AbstractGameLaunchHandler::getVendorClassName, Function.identity()));
    }

    public AbstractGameLaunchHandler<Object, Object> getHandler(String name) {
        @SuppressWarnings("unchecked")
        AbstractGameLaunchHandler<Object, Object> handler = (AbstractGameLaunchHandler<Object, Object>) handlerMap.get(name);
        return handler;
    }
}
