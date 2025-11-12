package com.nextgen.gameaggregator.vendor.facai.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonProperty("Result")
    private Integer result;
    @JsonProperty("ErrorText")
    private String errorText;

    public ErrorResponse(String responseCode) {
        this.result = Integer.valueOf(responseCode);
        this.errorText = ResponseCodes.RESPONSE_DESCRIPTION.get(responseCode);
    }
}
