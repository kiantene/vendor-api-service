package com.nextgen.gameaggregator.vendor.crystal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private Data data;
    private Error error;

    @Getter
    @Builder
    public static class Error {
        private String code;
        private String message;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Data {
    }


}
