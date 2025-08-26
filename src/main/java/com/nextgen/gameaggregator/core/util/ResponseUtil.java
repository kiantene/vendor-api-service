package com.nextgen.gameaggregator.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

public class ResponseUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseUtil() {

    }

    public static void writeErrorResponse(HttpServletResponse response, Object responseBody, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }

    public static String getResponseBody(ContentCachingResponseWrapper wrappedResponse, HttpServletResponse response)
            throws IOException {
        byte[] content = wrappedResponse.getContentAsByteArray();
        wrappedResponse.copyBodyToResponse();
        return new String(content, response.getCharacterEncoding());
    }

    public static VendorErrorResponse createDefaultErrorResponse(String body) {
        return new VendorErrorResponse(HttpStatus.UNAUTHORIZED, body);
    }
}
