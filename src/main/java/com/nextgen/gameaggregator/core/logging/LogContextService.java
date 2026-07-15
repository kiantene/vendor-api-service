package com.nextgen.gameaggregator.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.logging.ApiRequestLog;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogContextService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaService kafkaService;

    public void debug(String key, Object value) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) return;

        logContext.put(key, value);
    }

    public void log(String key, Object value) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) return;

        logContext.putCustomField(key, value);
    }

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

        if (logContext.getApiEnd() == 0) {
            logContext.setApiEnd(System.currentTimeMillis());
        }

        if (response == null) return;
        if (logContext.getApiResponse() == null) {
            logContext.setApiResponse(response.getBody());
            logContext.setApiStatusCode(response.getStatusCode().value());
        }
    }

    public static void populateLogContext(LogContext logContext, VendorPlayerAware vendorPlayerAware) {
        if (logContext == null || vendorPlayerAware == null) {
            log.warn("populateLogContext failed: logcontext or vendorPlayerAware is null");
            return;
        }

        logContext.setAgentId(vendorPlayerAware.getAgentId());
        logContext.setVendorId(vendorPlayerAware.getVendorId());
        logContext.setUsername(vendorPlayerAware.getAgentPlayerUsername());
    }

    // Backward compatible function
    public static void updateLogContextFromHttpRequestLog(LogContext logContext, HttpRequestLog httpRequestLog) {
        if (httpRequestLog == null) return;

        if (httpRequestLog.getBetEnd() == null) {
            httpRequestLog.setBetEnd(System.currentTimeMillis());
        }
        if (logContext.getStart() == 0 && httpRequestLog.getBetStart() != null) {
            logContext.setStart(httpRequestLog.getBetStart());
        }
        if (logContext.getEnd() == 0 && httpRequestLog.getBetEnd() != null) {
            logContext.setEnd(httpRequestLog.getBetEnd());
        }
        if (logContext.getApiStart() == 0 && httpRequestLog.getOperatorStart() != null) {
            logContext.setApiStart(httpRequestLog.getOperatorStart());
        }
        if (logContext.getApiEnd() == 0 && httpRequestLog.getOperatorEnd() != null) {
            logContext.setApiEnd(httpRequestLog.getOperatorEnd());
        }
        if (logContext.getApiStatusCode() == null) {
            logContext.setApiStatusCode(httpRequestLog.getOperatorHttpStatusCode());
        }
        logContext.put(HttpRequestLog.class.getSimpleName(), httpRequestLog);
    }

    // Backward compatible function
    public static HttpRequestLog toHttpRequestLog(LogContext logContext) {
        final Integer PROCESSING = 1;

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        logContext.setTraceId(httpRequestLog.getId());
        httpRequestLog.setUrl(logContext.getUrl());
        httpRequestLog.setRequestBody(logContext.getBody() == null ? "" : logContext.getBody().toString());
        httpRequestLog.setBetStart(System.currentTimeMillis());
        httpRequestLog.setStatus(PROCESSING);
        return httpRequestLog;
    }

    public void logApiRequest(LogContext logContext, String responseBody) {
        // This function will only apply to the following request types
        // WalletBalanceAction, WalletBetAction, WalletBetResultAction, WalletRollbackAction
        if (logContext.exists(HttpRequestLog.class.getSimpleName())) {
            HttpRequestLog httpRequestLog = (HttpRequestLog) logContext.get(HttpRequestLog.class.getSimpleName());
            if (httpRequestLog.getRequestType() == null) {
                httpRequestLog.setRequestType(logContext.getLogGroup());
            }
            httpRequestLog.setResponseBody(responseBody);
            httpRequestLog.setEndTime(System.currentTimeMillis());
            httpRequestLog.setOperatorUsername(logContext.getUsername());
            httpRequestLog.setVendorId(logContext.getVendorId());
            httpRequestLog.setAgentId(logContext.getAgentId());

            if (httpRequestLog.getOperatorEndPoints() == null) {
                httpRequestLog.setOperatorEndPoints(logContext.getApiUrl());
            }
            if (httpRequestLog.getOperatorStart() == null && logContext.getApiStart() > 0) {
                httpRequestLog.setOperatorStart(logContext.getApiStart());
            }
            if (httpRequestLog.getOperatorEnd() == null && logContext.getApiEnd() > 0) {
                httpRequestLog.setOperatorEnd(logContext.getApiEnd());
            }
            if (httpRequestLog.getBetStart() == null && logContext.getStart() > 0) {
                httpRequestLog.setBetStart(logContext.getStart());
            }
            if (httpRequestLog.getBetEnd() == null && logContext.getEnd() > 0) {
                httpRequestLog.setBetEnd(logContext.getEnd());
            }

            if (httpRequestLog.getOperatorData() == null) {
                try {
                    httpRequestLog.setOperatorData(objectMapper.writeValueAsString(logContext.getApiBody()));
                    if (logContext.getApiResponse() instanceof String) {
                        httpRequestLog.setOperatorResponse(logContext.getApiResponse().toString());
                    } else {
                        httpRequestLog.setOperatorResponse(objectMapper.writeValueAsString(logContext.getApiResponse()));
                    }
                } catch (Exception ex) {
                    httpRequestLog.setOperatorData(logContext.getApiBody().toString());
                    httpRequestLog.setOperatorResponse(logContext.getApiResponse().toString());
                }
            }

            if (logContext.getException() != null) {
                String exception = logContext.getException();
                httpRequestLog.setStatus(-1);
                httpRequestLog.setErrorMessage(exception);
                httpRequestLog.setExceptionMessage(logContext.getErrorMessage());
                httpRequestLog.setRootCause(logContext.getRootCause());
            } else {
                httpRequestLog.setStatus(2);
            }

            kafkaService.produceApiRequestLog(new ApiRequestLog(httpRequestLog));
            logContext.delete(HttpRequestLog.class.getSimpleName());
        }
    }
}
