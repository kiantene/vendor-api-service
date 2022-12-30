package com.nextgen.gameaggregator.event;

import java.util.HashMap;
import java.util.Map;

public class EventDispatcherSystem {

    private static final Map<Class<? extends Event>, EventDispatcher<?>> dispatchers = new HashMap<>();

    public static <T extends Event> void emit(T event) {
        @SuppressWarnings("unchecked")
        EventDispatcher<T> dispatcher = (EventDispatcher<T>) dispatchers.get(event.getClass());
        dispatcher.emit(event);
    }

    public static <T extends Event> void addListener(Class<T> eventClass, EventListener<T> listener) {
        @SuppressWarnings("unchecked")
        EventDispatcher<T> dispatcher = (EventDispatcher<T>) dispatchers.get(eventClass);
        if (dispatcher == null) {
            dispatcher = new EventDispatcher<>();
            dispatchers.put(eventClass, dispatcher);
        }
        dispatcher.addListener(listener);
    }
}
