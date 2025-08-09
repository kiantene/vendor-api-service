package com.nextgen.gameaggregator.core.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RequestParserService {

    private final ObjectMapper objectMapper;

    public RequestParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
            return objectMapper.readValue(rawBody, new TypeReference<>() {});
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
}
