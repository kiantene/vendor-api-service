package com.nextgen.gameaggregator.logging;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.Getter;

@Getter
public class ApiRequestBalanceLog extends ApiRequestLog {

    private String errorMessage;
    private String rootCause;

    public ApiRequestBalanceLog(HttpRequestLog httpRequestLog) {
        super(httpRequestLog);

        if (!httpRequestLog.getRootCause().isEmpty()) {
            this.rootCause = httpRequestLog.getRootCause();
        }

        if (!httpRequestLog.getExceptionMessage().isEmpty()) {
            this.errorMessage = httpRequestLog.getExceptionMessage();
        }
    }
}
