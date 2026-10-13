package com.nextgen.gameaggregator.promoengine;

import lombok.Getter;

@Getter
public class PromoEngineResponse<T> {
    /** Promo-engine serializes its {@code ResponseCodes} enum by name, so a success arrives as this string. */
    public static final String STATUS_OK = "SC_OK";

    private String traceId;
    private String status;
    private String message;
    private T data;
}
