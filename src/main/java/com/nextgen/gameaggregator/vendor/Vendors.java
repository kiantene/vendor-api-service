package com.nextgen.gameaggregator.vendor;

import java.util.HashMap;
import java.util.Map;

public enum Vendors {
    PRAGMATIC       (1, null, "pragmaticplay"),
    PGSOFT          (2, null, "pgsoft"),
    CQ9             (3, null, "cq9"),
    JILI            (4, null, "jili"),
    FACHAI          (5, null, "facai"),
    SPADEGAMING     (7, null, "spadegaming"),
    JDB             (8, null, "jdb")
    ;

    private static final int DEFAULT_TIMEOUT_MILLIS = 4000; // 4 seconds
    private static final String CALLBACK_PREFIX = "api/v1/";
    private final int id;
    private final Integer timeoutMillis;
    private final String className;

    Vendors(int id, Integer timeoutMillis, String className) {
        this.id = id;
        this.timeoutMillis = timeoutMillis;
        this.className = className;
    }

    public int getId() {
        return id;
    }

    public int getTimeoutMillis() {
        return timeoutMillis != null ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }

    public String getClassName() {
        return className;
    }

    public String getCallback() {
        return CALLBACK_PREFIX + className + "/";
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
