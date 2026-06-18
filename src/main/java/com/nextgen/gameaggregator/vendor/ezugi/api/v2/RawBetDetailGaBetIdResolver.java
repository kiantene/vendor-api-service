package com.nextgen.gameaggregator.vendor.ezugi.api.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

public final class RawBetDetailGaBetIdResolver {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BET_ID = "betId";

    private RawBetDetailGaBetIdResolver() {
    }

    public static Optional<String> resolve(Object operatorApiBody) {
        if (operatorApiBody == null) {
            return Optional.empty();
        }
        if (operatorApiBody instanceof Map<?, ?> map) {
            return toNonBlankString(map.get(BET_ID));
        }
        try {
            JsonNode node = operatorApiBody instanceof String body
                    ? OBJECT_MAPPER.readTree(body)
                    : OBJECT_MAPPER.valueToTree(operatorApiBody);
            return toNonBlankString(node.path(BET_ID).asText(null));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<String> toNonBlankString(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        String text = value.toString();
        return text.isBlank() ? Optional.empty() : Optional.of(text);
    }
}
