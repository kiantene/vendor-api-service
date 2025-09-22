package com.nextgen.gameaggregator.core.engine.promo.payout;

import org.springframework.stereotype.Component;

@Component
public class PromoPayoutContextHolder {
    private static final ThreadLocal<PromoPayoutWrapperContext> contextHolder = new ThreadLocal<>();

    private PromoPayoutContextHolder() {
    }

    public static void set(PromoPayoutWrapperContext context) {
        contextHolder.set(context);
    }

    public static PromoPayoutWrapperContext get() {
        return contextHolder.get();
    }

    public static PromoPayoutWrapperContext getRequired() {
        PromoPayoutWrapperContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("PromoPayoutWrapperContext not initialized");
        }
        return context;
    }

    public static void clear() {
        contextHolder.remove();
    }

    // Convenience methods for common access patterns
    public static PromoPayoutConfig getConfig() {
        return getRequired().getConfig();
    }

    public static PromoPayoutContext getContext() {
        return getRequired().getContext();
    }

    public static boolean isInitialized() {
        return contextHolder.get() != null;
    }
}
