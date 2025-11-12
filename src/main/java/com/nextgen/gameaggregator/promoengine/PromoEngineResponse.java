package com.nextgen.gameaggregator.promoengine;

import lombok.Getter;

@Getter
public class PromoEngineResponse<T> {
    private String traceId;
    private String status;
    private String message;
    private T data;
}
