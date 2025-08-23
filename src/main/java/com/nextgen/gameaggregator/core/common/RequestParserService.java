package com.nextgen.gameaggregator.core.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RequestParserService {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> parse(String contentType, String rawBody) {
        if (contentType == null) return Map.of();

        if (contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            return parseFormFields(rawBody);
        }

        if (contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            return parseJsonFields(rawBody);
        }

        return Map.of();
    }

    private Map<String, String> parseFormFields(String rawBody) {
        return Arrays.stream(rawBody.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        kv -> decode(kv[0]),
                        kv -> kv.length > 1 ? decode(kv[1]) : "",
                        (a, b) -> a // in case of duplicate keys
                ));
    }

    private Map<String, String> parseJsonFields(String rawBody) {
        try {
            return flattenJsonToStringMap(rawBody);
        } catch (Exception e) {
            // log warning if needed
            return Map.of();
        }
    }

    private String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, String> flattenJsonToStringMap(String rawBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawBody);
            Map<String, String> flatMap = new HashMap<>();
            flattenNode("", rootNode, flatMap);
            return flatMap;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void flattenNode(String prefix, JsonNode node, Map<String, String> flatMap) {
        if (node.isObject()) {
            // Handle nested objects
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
                flattenNode(key, field.getValue(), flatMap);
            }
        } else if (node.isArray()) {
            // Handle arrays
            for (int i = 0; i < node.size(); i++) {
                String key = prefix + "[" + i + "]";
                flattenNode(key, node.get(i), flatMap);
            }
        } else {
            // Handle primitive values (string, number, boolean, null)
            flatMap.put(prefix, node.asText());
        }
    }
}
