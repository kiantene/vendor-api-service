package com.nextgen.gameaggregator.vendor;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

//@formatter:off
public enum Vendors {
    // Add vendor according to id in sequence
    PRAGMATIC       (1,   null, "pragmaticplay", true),
    PGSOFT          (2,   null, "pgsoft",        false),
    CQ9             (3,   null, "cq9",           false),
    JILI            (4,   null, "jili",          false),
    FACHAI          (5,   null, "facai",         true),
    SPADEGAMING     (7,   null, "spadegaming",   false),
    JDB             (8,   null, "jdb",           true),
    MG              (17,  null, "mg",            false),
    RELAXGAMING     (18,  null, "dotconnections",true),
    HABANERO        (19,  null, "habanero",      true),
    EVOPLAY         (21,  null, "evoplay",       true),
    EZUGI           (24,  null, "ezugi",         true),
    YELLOWBAT       (30,  null, "yesbingo",      true),
    SLOTEGRATOR     (74, null, "slotegrator",    true),
    KOOLBET         (76,  null, "koolbet",       true),
    SABAPLAY        (83,  null, "sabaplay",      true),
    MTPOKER         (84,  null, "mtpoker",      true),
    BLAZEGAMING     (89,  null, "blazegaming",   true),
    INBETGAMES      (91,  null, "inbetgames",    true),
    BOMBAY          (42,  null, "bombay",        true),
    CRYSTAL         (94,  null, "crystal",       true),
    VPLUS           (97,  null, "vplus",         true),
    FUGASO          (101, null, "groove",        true),
    GALAXSYS        (102, null, "digitain",      true),
    IRONDOG         (103, null, "groove",        true),
    REVOLVER        (104, null, "groove",        true),
    TOPBET          (105, null, "topbet",        true),
    GPKEVOLUTION    (106, null, "gpkv2",         true),
    IDNPLAY         (111, null, "idnplay",       true),
    ONEBET          (114, null, "onebet",        true),
    EGT_DIGITAL     (115, null, "egtdigital",    true),
    WAZDAN          (116, null, "wazdan",        true),
    ENDORPHINA      (117, null, "endorphina",    true),
    VIVOGAMING      (118, null, "vivogaming",    true),
    MANCALA         (120, null, "mancala",       true),
    YESBINGO        (30,  null, "yesbingo",      false),
    POPIPLAY        (122, null, "popiplay",      true),
    MTLIVE          (125, null, "mtlive",        true),
    LUCKY365        (130, null, "lucky365",      true),
    HP100           (124, null, "hp100",         true),
    CF6             (137, null, "cockfight6",           true),
    AVIATOR_STUDIO  (96,  2000, "aviatorstudio", true)
    ;

    private static final int DEFAULT_TIMEOUT_MILLIS = 4000; // 4 seconds
    private static final String CALLBACK_PREFIX = "/api/v1/";
    // ---- lookup maps ----
    private static final Map<Integer, Vendors> BY_ID = new HashMap<>();

    static {
        for (Vendors v : values()) {
            BY_ID.put(v.id, v);
        }
    }

    @Getter
    private final int id;
    private final Integer timeoutMillis;
    @Getter
    private final String className;
    @Getter
    private final boolean newFramework;

    Vendors(int id, Integer timeoutMillis, String className, boolean newFramework) {
        this.id = id;
        this.timeoutMillis = timeoutMillis;
        this.className = className;
        this.newFramework = newFramework;
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

    public int getTimeoutMillis() {
        return timeoutMillis != null ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }

    public String getCallback() {
        return CALLBACK_PREFIX + className;
    }
}
