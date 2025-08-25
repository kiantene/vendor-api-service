package com.nextgen.gameaggregator.core.engine.game;

public interface GameSessionData {
    String getToken();
    String getVendorSessionToken();
    String getVendorPlayerUsername();
    default String getVendorGameCode() {
        return "";
    }
}
