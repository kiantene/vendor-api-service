package com.nextgen.gameaggregator.core.engine.game.authenticate;

import org.springframework.stereotype.Component;

@Component
public class AuthContextHolder {
    private static final ThreadLocal<AuthWrapperContext> contextHolder = new ThreadLocal<>();

    private AuthContextHolder() {

    }

    public static void set(AuthWrapperContext context) {
        contextHolder.set(context);
    }

    public static AuthWrapperContext get() {
        return contextHolder.get();
    }

    public static AuthWrapperContext getRequired() {
        AuthWrapperContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("AuthWrapperContext not initialized");
        }
        return context;
    }

    public static void clear() {
        contextHolder.remove();
    }

    // Convenience methods for common access patterns
    public static AuthConfig getConfig() {
        return getRequired().getConfig();
    }

    public static AuthenticateContext getAuthContext() {
        return getRequired().getAuthContext();
    }

    public static boolean isInitialized() {
        return contextHolder.get() != null;
    }
}
