package com.nextgen.gameaggregator.eventing.core;

import com.nextgen.gameaggregator.eventing.core.Event;

public interface EventListener<E extends Event> {

    void onEvent(E event);
}
