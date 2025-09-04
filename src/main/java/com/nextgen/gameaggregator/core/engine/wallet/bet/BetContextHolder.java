package com.nextgen.gameaggregator.core.engine.wallet.bet;

import org.springframework.stereotype.Component;

@Component
public class BetContextHolder {
    private static final ThreadLocal<BetWrapperContext> contextHolder = new ThreadLocal<>();

    private BetContextHolder() {

    }

    public static void set(BetWrapperContext context) {
        contextHolder.set(context);
    }

    public static BetWrapperContext get() {
        return contextHolder.get();
    }

    public static BetWrapperContext getRequired() {
        BetWrapperContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("BetResultWrapperContext not initialized");
        }
        return context;
    }

    public static void clear() {
        contextHolder.remove();
    }

    // Convenience methods for common access patterns
    public static BetConfig getConfig() {
        return getRequired().getConfig();
    }

    public static BetContext getBetContext() {
        return getRequired().getBetContext();
    }

    public static boolean isInitialized() {
        return contextHolder.get() != null;
    }
}
