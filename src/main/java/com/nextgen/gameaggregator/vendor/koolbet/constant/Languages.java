package com.nextgen.gameaggregator.vendor.koolbet.constant;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Languages {
    private static final Map<String, String> LANGUAGE_CODE_MAP = new HashMap<>();

    static {
        LANGUAGE_CODE_MAP.put("bn", "bn-IN");
        LANGUAGE_CODE_MAP.put("da", "da-DK");
        LANGUAGE_CODE_MAP.put("de", "de-DE");
        LANGUAGE_CODE_MAP.put("en", "en-US");
        LANGUAGE_CODE_MAP.put("es", "es-AR");
        LANGUAGE_CODE_MAP.put("fr", "fr-FR");
        LANGUAGE_CODE_MAP.put("gr", "gr-GR");
        LANGUAGE_CODE_MAP.put("hi", "hi-IN");
        LANGUAGE_CODE_MAP.put("id", "id-ID");
        LANGUAGE_CODE_MAP.put("it", "it-IT");
        LANGUAGE_CODE_MAP.put("jp", "ja-JP");
        LANGUAGE_CODE_MAP.put("ko", "ko-KR");
        LANGUAGE_CODE_MAP.put("ms", "ms-MY");
        LANGUAGE_CODE_MAP.put("my", "my-MM");
        LANGUAGE_CODE_MAP.put("nl", "nl-NL");
        LANGUAGE_CODE_MAP.put("pt", "pt-BR");
        LANGUAGE_CODE_MAP.put("ro", "ro-RO");
        LANGUAGE_CODE_MAP.put("ru", "ru-RU");
        LANGUAGE_CODE_MAP.put("sv", "sv-SE");
        LANGUAGE_CODE_MAP.put("ta", "ta-IN");
        LANGUAGE_CODE_MAP.put("th", "th-TH");
        LANGUAGE_CODE_MAP.put("tr", "tr-TR");
        LANGUAGE_CODE_MAP.put("ur", "ur-IN");
        LANGUAGE_CODE_MAP.put("vt", "vi-VN");
        LANGUAGE_CODE_MAP.put("cn", "zh-CN");
        LANGUAGE_CODE_MAP.put("tw", "zh-CN");
    }

    public static String getLanguageCode(String language) {
        return LANGUAGE_CODE_MAP.get(language);
    }
}
