package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.repository.HttpRequestLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
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

            BufferedReader reader = request.getReader();
            StringBuilder requestBody = new StringBuilder();

            int value;
            while((value = reader.read()) != -1) {
                requestBody.append((char) value);
            }
            log.info(requestBody.toString());

            httpRequestLog.setUrl(request.getRequestURI());
            httpRequestLog.setMethod(request.getMethod());
            httpRequestLog.setHeaders(headersJson);
            httpRequestLog.setRequestBody(requestBody.toString());
            httpRequestLog.setStatus(PROCESSING);
            httpRequestLog.setRequestIp(request.getRemoteAddr());
            httpRequestLog.setStartTime(System.currentTimeMillis());

            httpRequestLogRepository.save(httpRequestLog);
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
                requestLog.setStatus(COMPLETED);

                httpRequestLogRepository.save(requestLog);
            } catch (Exception exception) {
                log.error(exception.getMessage());
            }
        }
    }

    public static String getStackTrace(Exception exception) {
        final String NEWLINE = "\r\n";
        log.error(exception.getMessage());
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append(exception.getMessage()).append(NEWLINE+NEWLINE);
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

    public static <T> T convertQueryStringToDto(String queryString, Class<T> objectClass) {
        Map<String, String> queryParameterMap = new HashMap<>();
        String[] fields = queryString.split("&");

        for (String field : fields) {
            String[] kv = field.split("=");
            if (kv.length == 2) queryParameterMap.put(kv[0], kv[1]);
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(queryParameterMap, objectClass);
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
}
