package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.service.BaseVendorService;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class BetResultContextHolder {
    private static final ThreadLocal<BetResultWrapperContext> contextHolder = new ThreadLocal<>();

    private BetResultContextHolder() {}

    public static Initializer initialise() {
        BetResultWrapperContext current = contextHolder.get();
        if (current == null) {
            contextHolder.set(BetResultWrapperContext.empty());
        }
        return new Initializer();
    }

    public static void set(BetResultWrapperContext context) {
        contextHolder.set(context);
    }

    public static BetResultWrapperContext get() {
        return contextHolder.get();
    }

    public static BetResultWrapperContext getRequired() {
        BetResultWrapperContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("BetResultWrapperContext not initialized");
        }
        return context;
    }

    public static void clear() {
        contextHolder.remove();
    }

    // Convenience methods for common access patterns
    public static BetResultConfig getConfig() {
        return getRequired().getConfig();
    }

    public static BetResultContext getBetResultContext() {
        return getRequired().getBetResultContext();
    }

    public static BaseVendorService getVendorService() {
        return getRequired().getVendorService();
    }

    public static boolean isInitialized() {
        return contextHolder.get() != null;
    }

    public static final class Initializer {
        public Initializer configure(Consumer<BetResultConfig> consumer) {
            BetResultWrapperContext ctx = getRequired();
            consumer.accept(ctx.getConfig());
            return this;
        }
    }
}
