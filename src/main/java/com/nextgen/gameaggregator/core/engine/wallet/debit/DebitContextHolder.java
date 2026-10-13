package com.nextgen.gameaggregator.core.engine.wallet.debit;

import org.springframework.stereotype.Component;

/**
 * Thread-local holder for the per-request {@link DebitWrapperContext}, mirroring
 * {@link com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextHolder}.
 * <p>
 * Lifecycle: set by {@link WalletDebitServiceWrapper#initialise(DebitContext)} → cleared in finally.
 */
@Component
public class DebitContextHolder {
    private static final ThreadLocal<DebitWrapperContext> contextHolder = new ThreadLocal<>();

    private DebitContextHolder() {
    }

    public static void set(DebitWrapperContext context) {
        contextHolder.set(context);
    }

    public static DebitWrapperContext get() {
        return contextHolder.get();
    }

    public static DebitWrapperContext getRequired() {
        DebitWrapperContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("DebitWrapperContext not initialized");
        }
        return context;
    }

    public static void clear() {
        contextHolder.remove();
    }

    public static DebitConfig getConfig() {
        return getRequired().getConfig();
    }

    public static DebitContext getDebitContext() {
        return getRequired().getDebitContext();
    }

    public static boolean isInitialized() {
        return contextHolder.get() != null;
    }
}
