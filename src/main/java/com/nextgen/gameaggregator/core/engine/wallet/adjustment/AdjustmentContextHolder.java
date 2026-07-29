package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import org.springframework.stereotype.Component;

@Component
public class AdjustmentContextHolder {
    private static final ThreadLocal<AdjustmentWrapperContext> contextHolder = new ThreadLocal<>();

    private AdjustmentContextHolder() {}

    public static void set(AdjustmentWrapperContext context) {
        contextHolder.set(context);
    }

    public static AdjustmentWrapperContext get() {
        return contextHolder.get();
    }

    public static AdjustmentWrapperContext getRequired() {
        AdjustmentWrapperContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("AdjustmentWrapperContext not initialized");
        }
        return context;
    }

    public static void clear() {
        contextHolder.remove();
    }

    // Convenience methods for common access patterns
    public static AdjustmentConfig getConfig() {
        return getRequired().getConfig();
    }

    public static AdjustmentContext getAdjustmentContext() {
        return getRequired().getAdjustmentContext();
    }

    public static boolean isInitialized() {
        return contextHolder.get() != null;
    }
}
