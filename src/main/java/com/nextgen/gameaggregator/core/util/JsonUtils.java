package com.nextgen.gameaggregator.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Function;

public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JsonUtils() { }

    public static <T> T parseSafely(
            String rawJson,
            Class<T> type,
            Function<JsonProcessingException, ? extends RuntimeException> exceptionSupplier
    ) {
        try {
            return objectMapper.readValue(rawJson, type);
        } catch (JsonProcessingException e) {
            throw exceptionSupplier.apply(e);
        }
    }

    public static <T> String toJson(T object) {
        if (object == null) throw new IllegalArgumentException("JSON serialize failed: object is null");

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("JSON serialize failed: " + exception.getMessage(), exception);
        }
    }
}
