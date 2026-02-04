package com.nextgen.gameaggregator.game.launcher.vplus;

public class TokenHolder {
    private static final ThreadLocal<String> tokenHolderThread = new ThreadLocal<>();

    private TokenHolder() {
    }

    public static String getToken() {
        return tokenHolderThread.get();
    }

    public static void setToken(String body) {
        tokenHolderThread.set(body);
    }

    public static void clear() {
        tokenHolderThread.remove();
    }
}
