package com.nextgen.gameaggregator.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

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
}
