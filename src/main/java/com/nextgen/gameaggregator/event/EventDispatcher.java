package com.nextgen.gameaggregator.event;

import com.nextgen.gameaggregator.service.ConcurrencyService;

import java.util.ArrayList;
import java.util.List;

public class EventDispatcher<T extends Event> {

    private final List<EventListener<T>> listeners = new ArrayList<>();

    public void emit(T event) {
        for (EventListener<T> listener : listeners) {
            ConcurrencyService.THREAD_POOL.submit(() -> {
                listener.onEvent(event);
            });
        }
    }

    public void addListener(EventListener<T> listener) {
        listeners.add(listener);
    }
}
