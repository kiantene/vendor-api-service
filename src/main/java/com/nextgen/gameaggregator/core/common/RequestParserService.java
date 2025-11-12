package com.nextgen.gameaggregator.core.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.InvalidRequestException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RequestParserService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FORM = MediaType.APPLICATION_FORM_URLENCODED_VALUE; // "application/x-www-form-urlencoded"
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;            // "application/json"

    public Map<String, String> parse(String contentType, String rawBody) {
        if (contentType == null) return Map.of();
        if (contentType.startsWith(FORM)) return parseFormFields(rawBody);
        if (contentType.startsWith(JSON)) return parseJsonFields(rawBody);
        return Map.of();
    }

    private Map<String, String> parseFormFields(String rawBody) {
        // Handle edge cases quickly
        if (rawBody.isBlank()) return Map.of();

        return Arrays.stream(rawBody.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        kv -> decode(kv[0]),
                        kv -> kv.length > 1 ? decode(kv[1]) : "",
                        (a, b) -> a,                 // keep first occurrence
                        LinkedHashMap::new           // deterministic order
                ));
    }

    private Map<String, String> parseJsonFields(String rawBody) {
        try {
            return flattenJsonToStringMap(rawBody);
        } catch (Exception e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    private String decode(String s) {
        if (s == null) return "";
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Return original token instead of erasing it
            return s;
        }
    }

    private Map<String, String> flattenJsonToStringMap(String rawBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawBody);
        Map<String, String> flat = new LinkedHashMap<>();
        flattenNode("", root, flat);
        return flat;
    }

    private void flattenNode(String prefix, JsonNode node, Map<String, String> flat) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenNode(key, entry.getValue(), flat);
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String key = prefix + "[" + i + "]";
                flattenNode(key, node.get(i), flat);
            }
        } else {
            String value = node.isNull() ? "" : node.asText();
            flat.put(prefix, value);
        }
    }
}
