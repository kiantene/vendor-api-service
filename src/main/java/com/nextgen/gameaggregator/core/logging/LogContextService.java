package com.nextgen.gameaggregator.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.logging.ApiRequestLog;
import com.nextgen.gameaggregator.service.KafkaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogContextService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaService kafkaService;

    public void logStart(String url, Object body) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) return;

        logContext.setApiStart(System.currentTimeMillis());
        logContext.setApiBody(body);
        logContext.setApiUrl(url);
    }

    public void logEnd(ResponseEntity<String> response) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) return;

        logContext.setApiEnd(System.currentTimeMillis());

        if (response == null) return;
        logContext.setApiResponse(response.getBody());
        logContext.setApiStatusCode(response.getStatusCode().value());
    }

    public void logApiRequest(LogContext logContext, HttpServletRequest request, String responseBody) {
        // This function will only apply to the following request types
        // WalletBalanceAction, WalletBetAction, WalletBetResultAction, WalletRollbackAction
        if (logContext.exists(HttpRequestLog.class.getSimpleName())) {
            HttpRequestLog httpRequestLog = (HttpRequestLog) logContext.get(HttpRequestLog.class.getSimpleName());
            if (request.getAttribute("rawBody") != null) {
                httpRequestLog.setRequestBody(request.getAttribute("rawBody").toString());
            }
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
}
