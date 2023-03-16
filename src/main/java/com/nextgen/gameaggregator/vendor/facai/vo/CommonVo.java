package com.nextgen.gameaggregator.vendor.facai.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    private ResponseCodes responseCodes;

    public CommonVo() {
        this.setSuccessResponseCode(ResponseCodes.SUCCESS);
    }

    public void setSuccessResponseCode(ResponseCodes responseCode) {
        this.responseCodes = responseCode;
        this.result = responseCode.code;
    }

    public void setErrorResponseCode(ResponseCodes responseCode) {
        this.responseCodes = responseCode;
        this.result = responseCode.code;
        this.errorText = responseCode.description;
    }
    @Override
    public boolean hasError() {
        return false;
    }
}
