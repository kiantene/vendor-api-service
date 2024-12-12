package com.nextgen.gameaggregator.exception;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import lombok.Getter;

@Getter
public class DuplicateRequestException extends Exception {

    private final transient RequestIdempotentLog requestIdempotentLog;

    public DuplicateRequestException() {
        super();
        this.requestIdempotentLog = null;
    }

    public DuplicateRequestException(String message) {
        super(message);
        this.requestIdempotentLog = null;
    }

    public DuplicateRequestException(RequestIdempotentLog requestIdempotentLog) {
        super("Duplicate request with " + requestIdempotentLog.getId());
        this.requestIdempotentLog = requestIdempotentLog;
    }
}
