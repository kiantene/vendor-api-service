package com.nextgen.gameaggregator.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.logging.ApiRequestLog;
import com.nextgen.gameaggregator.service.KafkaService;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingManager {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaService kafkaService;
    private static final Integer THREAD_SIZE = 32;
    private final ExecutorService asyncLogger = Executors.newFixedThreadPool(THREAD_SIZE);

    public LogContext onRequestStart(HttpServletRequest request) {
        LogContext logContext = new LogContext();
        logContext.setUrl(request.getRequestURI());
        LogContextHolder.set(logContext);

        return logContext;
    }

    public void onRequestHandled(HttpServletRequest request) {
        // Optional: populate controller result-related info
    }

    public void onRequestCompleted(HttpServletRequest request, String responseBody, Exception ex) {
        LogContext logContext = LogContextHolder.get();
        if (logContext != null) {
            this.logAsync(logContext, responseBody, ex);

            // for backward compatibility with httpRequestLog/apiRequestLog, will be removed in the future
            this.logApiRequest(logContext, request, responseBody);
        }
    }

    public void onExceptionThrown(Exception ex) {
        LogContext logContext = LogContextHolder.get();
        if (logContext != null) {
            logContext.setException(ex.getClass().getName());
            logContext.setErrorMessage(ex.getMessage());
        }
    }

    private void logAsync(LogContext logContext, String responseBody, Exception ex) {
        logContext.setEnd();
        if (responseBody != null && !responseBody.isEmpty()) {
            logContext.setResponse(responseBody);
        }
        if (ex != null && logContext.getException() == null) {
            logContext.setException(ex);
        }
        final String logJson = logContext.toJson();
        boolean hasException = logContext.getException() != null;
        LogContextHolder.clear();

        asyncLogger.submit(() -> {
            try {
                if (hasException) {
                    log.error(logJson);
                } else {
                    log.info(logJson);
                }
            } catch (Exception e) {
                log.error("Failed to log asynchronously: ", e);
            }
        });
    }

    private void logApiRequest(LogContext logContext, HttpServletRequest request, String responseBody) {
        // This function will only apply to the following request types
        // WalletBalanceAction, WalletBetAction, WalletBetResultAction, WalletRollbackAction
        if (logContext.exists(HttpRequestLog.class.getSimpleName())) {
            HttpRequestLog httpRequestLog = (HttpRequestLog) logContext.get(HttpRequestLog.class.getSimpleName());
//            if (request.getAttribute("rawBody") != null) {
//                httpRequestLog.setRequestBody(request.getAttribute("rawBody").toString());
//            }
            httpRequestLog.setUrl(request.getRequestURI());
            httpRequestLog.setMethod(request.getMethod());
            httpRequestLog.setRequestIp(request.getRemoteAddr());
            httpRequestLog.setResponseBody(responseBody);
            httpRequestLog.setEndTime(System.currentTimeMillis());
            httpRequestLog.setOperatorStart(logContext.getApiStart());
            httpRequestLog.setOperatorEnd(logContext.getApiEnd());
            try {
                httpRequestLog.setOperatorData(objectMapper.writeValueAsString(logContext.getApiBody()));
                httpRequestLog.setOperatorResponse(objectMapper.writeValueAsString(logContext.getApiResponse()));
            } catch (Exception ex) {
                httpRequestLog.setOperatorData(logContext.getApiBody().toString());
                httpRequestLog.setOperatorData(logContext.getApiResponse().toString());
            }

            if (logContext.getException() != null) {
                String exception = logContext.getException();
                httpRequestLog.setStatus(-1);
                httpRequestLog.setErrorMessage(exception);
                httpRequestLog.setExceptionMessage(logContext.getErrorMessage());
                httpRequestLog.setRootCause(logContext.getRootCause());
            }

            kafkaService.produceApiRequestLog(new ApiRequestLog(httpRequestLog));
        }
    }

    @PreDestroy
    public void shutdown() {
        asyncLogger.shutdown();
    }
}
