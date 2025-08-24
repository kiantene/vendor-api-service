package com.nextgen.gameaggregator.vendor;

import java.util.HashMap;
import java.util.Map;

public enum Vendors {
    // Add vendor according to id in sequence
    PRAGMATIC       (1,   null, "pragmaticplay", false),
    PGSOFT          (2,   null, "pgsoft",        false),
    CQ9             (3,   null, "cq9",           false),
    JILI            (4,   null, "jili",          false),
    FACHAI          (5,   null, "facai",         true),
    SPADEGAMING     (7,   null, "spadegaming",   false),
    JDB             (8,   null, "jdb",           false),
    EZUGI           (24,  null, "ezugi",         true),
    CRYSTAL         (94,  null, "crystal",       true),
    AVIATOR_STUDIO  (96,  2000, "aviatorstudio", true),
    VPLUS           (97,  null, "vplus",         true)
    ;

    private static final int DEFAULT_TIMEOUT_MILLIS = 4000; // 4 seconds
    private static final String CALLBACK_PREFIX = "api/v1/";
    private final int id;
    private final Integer timeoutMillis;
    private final String className;
    private final boolean newFramework;

    Vendors(int id, Integer timeoutMillis, String className, boolean newFramework) {
        this.id = id;
        this.timeoutMillis = timeoutMillis;
        this.className = className;
        this.newFramework = newFramework;
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

    public boolean isNewFramework() {
        return newFramework;
    }

    public String getCallback() {
        return CALLBACK_PREFIX + className + "/";
    }

    // ---- lookup maps ----
    private static final Map<Integer, Vendors> BY_ID = new HashMap<>();

    static {
        for (Vendors v : values()) {
            BY_ID.put(v.id, v);
        }
    }

    public static Vendors fromId(int id) {
        return BY_ID.get(id); // can return null
    }

    public static int getTimeoutById(int id) {
        Vendors v = fromId(id);
        return (v != null) ? v.getTimeoutMillis() : DEFAULT_TIMEOUT_MILLIS;
    }

    public static boolean isNewFramework(int id) {
        Vendors v = fromId(id);
        return v != null && v.isNewFramework();
    }

    public static Vendors fromRequestURI(String requestURI) {
        if (requestURI == null) return null;
        for (Vendors v : values()) {
            if (requestURI.startsWith(v.getCallback())) {
                return v;
            }
        }
        return null;
    }
}
