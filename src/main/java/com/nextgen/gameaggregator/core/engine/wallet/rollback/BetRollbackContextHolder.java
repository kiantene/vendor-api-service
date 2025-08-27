package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import org.springframework.stereotype.Component;

@Component
public class BetRollbackContextHolder {
    private static final ThreadLocal<BetRollbackWrapperContext> contextHolder = new ThreadLocal<>();

    private BetRollbackContextHolder() {}

    public static void set(BetRollbackWrapperContext context) {
        contextHolder.set(context);
    }

    public static BetRollbackWrapperContext get() {
        return contextHolder.get();
    }

    public static BetRollbackWrapperContext getRequired() {
        BetRollbackWrapperContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("BetRollbackWrapperContext not initialized");
        }
        return context;
    }

    public static void clear() {
        contextHolder.remove();
    }

    // Convenience methods for common access patterns
    public static BetRollbackConfig getConfig() {
        return getRequired().getConfig();
    }

    public static BetRollbackContext getBetRollbackContext() {
        return getRequired().getBetRollbackContext();
    }

    public static boolean isInitialized() {
        return contextHolder.get() != null;
    }
}
