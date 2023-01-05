package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.repository.HttpRequestLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HttpService {
    public static final Integer PROCESSING = 1;
    public static final Integer COMPLETED = 2;
    public static final Integer ERROR = -1;

    @Autowired
    private HttpRequestLogRepository httpRequestLogRepository;

    public HttpRequestLog logRequest(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = new HttpRequestLog();

        try {
            Map<String, String> headers = this.getHeadersInfo(request);
            String headersJson = new ObjectMapper().writeValueAsString(headers);
            String requestBody = this.getRawRequestBody(request);
            log.info(requestBody);

            httpRequestLog.setUrl(request.getRequestURI());
            httpRequestLog.setMethod(request.getMethod());
            httpRequestLog.setHeaders(headersJson);
            httpRequestLog.setRequestBody(requestBody);
            httpRequestLog.setStatus(PROCESSING);
            httpRequestLog.setRequestIp(request.getRemoteAddr());
            httpRequestLog.setStartTime(System.currentTimeMillis());
        } catch (Exception exception) {
            log.error(exception.getMessage());
        }

        return httpRequestLog;
    }

    public void logResponse(HttpRequestLog requestLog, Object responseVo, String traceId) {
        if (requestLog != null) {
            try {
                String responseBody = new ObjectMapper().writeValueAsString(responseVo);
                requestLog.setResponseBody(responseBody);
                requestLog.setTraceId(traceId);
                if (requestLog.getEndTime() == null) {
                    requestLog.setEndTime(System.currentTimeMillis());
                }
                requestLog.setTimeTaken(requestLog.getEndTime() - requestLog.getStartTime());
                if (requestLog.getStatus().equals(PROCESSING)) {
                    requestLog.setStatus(COMPLETED);
                }

                httpRequestLogRepository.save(requestLog);
            } catch (Exception exception) {
                log.error(exception.getMessage());
            }
        }
    }

    public static String getStackTrace(Exception exception) {
        final String NEWLINE = "\r\n";
        log.error(exception.toString());
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append("Exception: ").append(exception).append(NEWLINE+NEWLINE);
        StackTraceElement[] stackTraceElements = exception.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            stackTrace.append(stackTraceElement).append(NEWLINE);
        }
        return stackTrace.toString();
    }

    public static <T> T convertJsonToDto(String json, Class<T> objectClass) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, objectClass);
    }

    public static <T> T convertQueryStringToDto(String queryString, Class<T> objectClass) throws InvalidRequestException {
        Map<String, String> queryParameterMap = new HashMap<>();
        String[] fields = queryString.split("&");

        for (String field : fields) {
            String[] kv = field.split("=");
            if (kv.length == 2) queryParameterMap.put(kv[0], kv[1]);
        }

        ObjectMapper mapper = new ObjectMapper();
        T object;

        // TODO: To review on this exception handling
        try {
            object = mapper.convertValue(queryParameterMap, objectClass);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new InvalidRequestException(); // re-throw as InvalidRequest
        }

        return object;
    }

    private Map<String, String> getHeadersInfo(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }

        return map;
    }

    private String getRawRequestBody(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder requestBody = new StringBuilder();
        int value;
        while((value = reader.read()) != -1) {
            requestBody.append((char) value);
        }
        return requestBody.toString();
    }
}
