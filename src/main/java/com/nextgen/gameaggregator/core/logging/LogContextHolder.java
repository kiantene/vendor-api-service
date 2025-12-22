package com.nextgen.gameaggregator.core.logging;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    public static String getVendorClassName() {
        LogContext ctx = contextHolder.get();

        if (ctx == null) {
            log.warn("No LogContext is available");
            return null;
        }

        String className = ctx.getVendorClassName();
        if (className == null) {
            log.warn("Vendor class name is not available");
        }

        return className;
    }
}
