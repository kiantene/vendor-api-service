package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.repository.HttpRequestLogRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class HttpService {
    public static final Integer PROCESSING = 1;
    public static final Integer COMPLETED = 2;
    public static final Integer ERROR = -1;

    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);

    @Value("${logging.http-request:true}")
    private Boolean enableHttpRequestLog;
    @Autowired
    private HttpRequestLogRepository httpRequestLogRepository;

    public HttpRequestLog start(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = new HttpRequestLog();
        try {
            Map<String, String> headers = this.getHeadersInfo(request);
            String headersJson = new ObjectMapper().writeValueAsString(headers);
            String requestBody = this.getRawRequestBody(request);
            httpRequestLog.setId(UUID.randomUUID().toString());
            httpRequestLog.setUrl(request.getRequestURI());
            httpRequestLog.setMethod(request.getMethod());
            httpRequestLog.setHeaders(headersJson);
            httpRequestLog.setRequestBody(requestBody);
            httpRequestLog.setStatus(PROCESSING);
            httpRequestLog.setRequestIp(request.getRemoteAddr());
            httpRequestLog.setStartTime(System.currentTimeMillis());
        } catch (Exception exception) {
            log.error(exception.getMessage());
            exception.printStackTrace();
        }

        return httpRequestLog;
    }

    public void end(HttpRequestLog requestLog, HttpResponse responseVo) {
        if (!enableHttpRequestLog) return;

        if (requestLog != null && responseVo != null) {
            requestLog.setEndTime(System.currentTimeMillis());
                THREAD_POOL.submit(() -> {
                    try {
                        String responseBody = new ObjectMapper().writeValueAsString(responseVo);
                        requestLog.setResponseBody(responseBody);
                        requestLog.setTimeTaken(requestLog.getEndTime() - requestLog.getStartTime());
                        requestLog.setStatus(!responseVo.hasError() ? COMPLETED : ERROR);
                        if (requestLog.getBetProcessEndTime() != null) {
                            requestLog.setBetProcessTimeTaken(requestLog.getBetProcessEndTime() - requestLog.getBetProcessStartTime());
                        }
                        if (requestLog.getOperatorProcessEndTime() != null) {
                            requestLog.setOperatorProcessTimeTaken(requestLog.getOperatorProcessEndTime() - requestLog.getOperatorProcessStartTime());
                        }

                        if (requestLog.getOperatorProcessEndTime() != null) {
                            requestLog.setOperatorProcessTimeTaken(requestLog.getOperatorProcessEndTime() - requestLog.getOperatorProcessStartTime());
                        }

                        if (requestLog.getBetProcessEndTime() != null) {
                            Long operatorProcessTime = Optional.ofNullable(requestLog.getOperatorProcessTimeTaken()).orElse(0L);
                            requestLog.setBetProcessTimeTaken(requestLog.getBetProcessEndTime() - requestLog.getBetProcessStartTime() - operatorProcessTime);
                        }

                        if (requestLog.getOperatorProcessEndTime() != null) {
                            requestLog.setOperatorProcessTimeTaken(requestLog.getOperatorProcessEndTime() - requestLog.getOperatorProcessStartTime());
                        }

                        if (requestLog.getBetProcessEndTime() != null) {
                            Long operatorProcessTime = Optional.ofNullable(requestLog.getOperatorProcessTimeTaken()).orElse(0L);
                            requestLog.setBetProcessTimeTaken(requestLog.getBetProcessEndTime() - requestLog.getBetProcessStartTime() - operatorProcessTime);
                        }

                        httpRequestLogRepository.save(requestLog);
                    } catch (Exception exception) {
                        log.error(exception.getMessage());
                        exception.printStackTrace();
                    }
                });
        } else {
            log.warn("HttpService.end: requestLog or responseVo is null");
        }
    }

    public void logError(HttpRequestLog requestLog, Exception exception) {
        if (requestLog != null) {
            String stackTrace = HttpService.getStackTrace(exception);
            requestLog.setStatus(ERROR);
            requestLog.setErrorMessage(stackTrace);
        } else {
            log.warn("HttpService.logError: requestLog is null");
            exception.printStackTrace();
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

    public static <T> T convertJsonToDto(String json, TypeReference<T> valueTypeRef) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, valueTypeRef);
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

    public static <T> T convertQueryStringToDtoUrlDecode(String queryString, Class<T> objectClass) throws InvalidRequestException {
//        Map<String, String> queryParameterMap = new HashMap<>();
        Map<String, Object> queryParameterMap = new HashMap<>();

        // TODO: To review on this exception handling
        try {
            queryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String[] fields = queryString.split("&");

        for (String field : fields) {
            String[] kv = field.split("=");
            if (kv.length == 2) {
                Object currentValue = queryParameterMap.get(kv[0]);
                if (currentValue == null) {
                    queryParameterMap.put(kv[0], kv[1]);
                } else if (currentValue instanceof String) {
                    String[] values = { (String) currentValue, kv[1] };
                    queryParameterMap.put(kv[0], values);
                } else if (currentValue instanceof String[]) {
                    String[] values = (String[]) currentValue;
                    Integer newLength = values.length + 1;
                    String[] newValues = Arrays.copyOf(values, newLength);
                    newValues[newLength - 1] = kv[1];
                    queryParameterMap.put(kv[0], newValues);
                }
            }
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
