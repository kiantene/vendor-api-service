package com.nextgen.gameaggregator.vendor.pragmaticplayv2.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.ResponseCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Integer error;      // Response status
    private String description; // Response status short description

    public ErrorResponse(ResponseCode responseCode) {
        this.error = responseCode.code;
        this.description = responseCode.description;
    }
}
