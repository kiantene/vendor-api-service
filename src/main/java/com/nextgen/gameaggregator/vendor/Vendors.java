package com.nextgen.gameaggregator.vendor;

import java.util.HashMap;
import java.util.Map;

public enum Vendors {
    PRAGMATIC_PLAY(1, null),
    PGSOFT(2, null),
    CQ9(3, null),
    JILI(4, null),
    FACHAI(5, null),
    SPADEGAMING(7, null),
    JDB(8, null)
    ;

    private static final int DEFAULT_TIMEOUT_MILLIS = 4000; // 4 seconds
    private final int id;
    private final Integer timeoutMillis; // nullable

    Vendors(int id, Integer timeoutMillis) {
        this.id = id;
        this.timeoutMillis = timeoutMillis;
    }

    public int getId() {
        return id;
    }

    public int getTimeoutMillis() {
        return timeoutMillis != null ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }

    private static final Map<Integer, Vendors> BY_ID = new HashMap<>();

    static {
        for (Vendors v : values()) BY_ID.put(v.id, v);
    }

    public static Vendors fromId(int id) {
        return BY_ID.get(id); // can return null
    }

    public static int getTimeoutById(int id) {
        Vendors v = fromId(id);
        return (v != null) ? v.getTimeoutMillis() : DEFAULT_TIMEOUT_MILLIS;
    }
}
