package com.nextgen.gameaggregator.core.common;

public interface OperatorRequestObject {
    String getTraceId();
    String getUsername();
    String getCurrency();
    Long getTimestamp();
}
