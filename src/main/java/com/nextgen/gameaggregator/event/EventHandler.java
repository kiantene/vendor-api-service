package com.nextgen.gameaggregator.event;

public interface EventHandler {
    void on(String type, String data);
}
