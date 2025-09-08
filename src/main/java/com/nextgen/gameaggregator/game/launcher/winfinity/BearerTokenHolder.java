package com.nextgen.gameaggregator.game.launcher.winfinity;

public class BearerTokenHolder {
    private static final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

    public static void setBody(String body) {
        bearerTokenHolder.set(body);
    }

    public static String getBody() {
        return bearerTokenHolder.get();
    }

    public static void clear() {
        bearerTokenHolder.remove();
    }
}
