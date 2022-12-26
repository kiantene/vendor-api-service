package com.nextgen.gameaggregator.event;

import com.nextgen.gameaggregator.service.ConcurrencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
public class EventDispatcher {
    private static final MultiValueMap<String, EventHandler> listeners = new LinkedMultiValueMap<>();

    public static <T> void addListener(Class<T> event, EventHandler handler) {
        addListener(event.getName(), handler);
    }

    public static void addListener(String event, EventHandler handler) {
        log.info("Adding event listener: " + event);
        listeners.add(event, handler);
    }

    public <T> void emit(Class<T> event, String data) {
        emit(event.getName(), data);
    }

    public void emit(String event, String data) {
        log.info("Emit event: " + event);
        if (listeners.containsKey(event)) {
            listeners.get(event).forEach(handler -> {
                ConcurrencyService.THREAD_POOL.submit(() -> {
                    try {
                        handler.on(event, data);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
            });
        }
    }
}
