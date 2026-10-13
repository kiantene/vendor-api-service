package com.nextgen.gameaggregator.core.engine.wallet.credit;

import org.springframework.stereotype.Component;

/** Thread-local holder for the per-request {@link CreditWrapperContext}. */
@Component
public class CreditContextHolder {
    private static final ThreadLocal<CreditWrapperContext> contextHolder = new ThreadLocal<>();

    private CreditContextHolder() {
    }

    public static void set(CreditWrapperContext context) {
        contextHolder.set(context);
    }

    public static CreditWrapperContext get() {
        return contextHolder.get();
    }

    public static CreditWrapperContext getRequired() {
        CreditWrapperContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("CreditWrapperContext not initialized");
        }
        return context;
    }

    public static void clear() {
        contextHolder.remove();
    }

    public static CreditConfig getConfig() {
        return getRequired().getConfig();
    }

    public static CreditContext getCreditContext() {
        return getRequired().getCreditContext();
    }

    public static boolean isInitialized() {
        return contextHolder.get() != null;
    }
}
