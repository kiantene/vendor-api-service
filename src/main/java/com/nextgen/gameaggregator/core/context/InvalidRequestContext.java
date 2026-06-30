package com.nextgen.gameaggregator.core.context;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Builder
public class InvalidRequestContext {
    private HttpServletRequest request;
    @Getter private Map<String, String> parsedFields;
    @Getter private Map<String, Object> responseBody;

    public static InvalidRequestContext of(HttpServletRequest request,
                                           Map<String, String> parsedFields,
                                           Map<String, Object> responseBody) {
        return InvalidRequestContext.builder()
                .request(request)
                .parsedFields(Map.copyOf(parsedFields))
                // Map.copyOf rejects null values; responseBody may contain nulls from POJO conversion.
                .responseBody(Collections.unmodifiableMap(new LinkedHashMap<>(responseBody)))
                .build();
    }

    /**
     * Retrieves the request URI.
     */
    public Optional<String> getRequestUri() {
        return Optional.ofNullable(request).map(HttpServletRequest::getRequestURI);
    }

    /**
     * Retrieves the HTTP method.
     */
    public Optional<String> getMethod() {
        return Optional.ofNullable(request).map(HttpServletRequest::getMethod);
    }

    /**
     * Retrieves the request content type.
     */
    public Optional<String> getContentType() {
        return Optional.ofNullable(request).map(HttpServletRequest::getContentType);
    }

    /**
     * Retrieves the value of a specific request header.
     */
    public Optional<String> getHeader(String name) {
        return Optional.ofNullable(request).map(r -> r.getHeader(name));
    }

    /**
     * Retrieves the value of a specific query parameter.
     */
    public Optional<String> getQueryParam(String name) {
        return Optional.ofNullable(request).map(r -> r.getParameter(name));
    }
}
