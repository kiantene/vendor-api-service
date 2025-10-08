package com.nextgen.gameaggregator.vendor.crystal.response;

import com.nextgen.gameaggregator.vendor.crystal.constant.ResponseCodes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private Error error;
    private final String data; // vendor is expecting null value

    @Getter
    @AllArgsConstructor
    public static class Error {
        private String code;
        private String message;

        public static Error of(ResponseCodes responseCode) {
            return new Error(
                    String.valueOf(responseCode.getCode()),
                    responseCode.getMessage()
            );
        }
    }
}
