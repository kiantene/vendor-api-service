package com.nextgen.gameaggregator.vendor.facai.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {

    @JsonProperty("Result")
    private Integer result;
    @JsonProperty("MainPoints")
    private Double mainPoints;
    @JsonProperty("ErrorText")
    private String errorText;

    public CommonVo() {
        this.setSuccessResponseCode(ResponseCodes.SUCCESS);
    }

    public void setSuccessResponseCode(String responseCode) {
        this.result = Integer.valueOf(responseCode);
    }

    public void setErrorResponseCode(String responseCode) {
        this.result = Integer.valueOf(responseCode);
        this.errorText = ResponseCodes.RESPONSE_DESCRIPTION.get(responseCode);
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
