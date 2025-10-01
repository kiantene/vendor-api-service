package com.nextgen.gameaggregator.game.launcher.winfinity.bearer;

public class BearerTokenHolder {
    private BearerTokenHolder() {}

    private static final ThreadLocal<String> tokenHolder = new ThreadLocal<>();

    public static void setToken(String body) {
        tokenHolder.set(body);
    }

    public static String getToken() {
        return tokenHolder.get();
    }

    public static void clear() {
        tokenHolder.remove();
    }
}
