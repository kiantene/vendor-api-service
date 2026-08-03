package com.nextgen.gameaggregator.game.launcher.endorphina.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

public class VendorUtil {

    private static final String DEFAULT_LANGUAGE = "en";

    private VendorUtil() {}

    public static String getSignature(Map<String, String> params, String salt) {
        List<String> keys = params.keySet().stream()
                .filter(k -> !"sign".equalsIgnoreCase(k))
                .sorted()
                .collect(Collectors.toList());

        List<String> values = new ArrayList<>();
        for (String key : keys) {
            Object value = params.get(key);
            values.add(value == null ? "" : value.toString());
        }
        String combine = String.join("", values);
        return combine + salt;
    }

    public static Map<String, String> getMergeRequestParams(HttpServletRequest request, Map<String, String> formFields) {
        Map<String, String> combined = new HashMap<>();

        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                combined.put(key, values[0]);
            }
        });

        if (formFields != null) {
            combined.putAll(formFields);
        }

        return combined;
    }

    public static Map<String, String> buildSortedParams(String exitUrl, String lang, String token, String nodeId) {
        String language = (lang == null || lang.isBlank()) ? DEFAULT_LANGUAGE : lang;

        Map<String, String> sortedParams = new TreeMap<>();
        sortedParams.put("exit", Objects.toString(exitUrl, ""));
        sortedParams.put("lang", language);
        sortedParams.put("nodeId", Objects.toString(nodeId, ""));
        sortedParams.put("token", Objects.toString(token, ""));
        return sortedParams;
    }

    public static String removeDash(String text) {
        return text == null ? "" : text.replace("-", "");
    }
}