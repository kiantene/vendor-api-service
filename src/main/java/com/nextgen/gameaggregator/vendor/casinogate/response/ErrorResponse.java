package com.nextgen.gameaggregator.vendor.casinogate.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private final int code;
    private final String description;
}
