package com.nextgen.gameaggregator.core.logging;

public class LogContextHolder {

    private LogContextHolder() {}

    private static final ThreadLocal<LogContext> contextHolder = new ThreadLocal<>();

    public static void set(LogContext context) {
        contextHolder.set(context);
    }

    public static LogContext get() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }
}
