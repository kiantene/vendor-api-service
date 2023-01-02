package com.nextgen.gameaggregator.eventing.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventDispatcher<T extends Event> {

    // TODO: to review whether THREAD_POOL should move to EventDispatcherSystem
    private static final Integer THREAD_SIZE = 8;
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);

    private final List<EventListener<T>> listeners = new ArrayList<>();

    public void emitAsync(T event) {
        listeners.forEach(listener -> {
            THREAD_POOL.submit(() -> {
                listener.onEvent(event);
            });
        });
    }

    public void addListener(EventListener<T> listener) {
        listeners.add(listener);
    }
}
