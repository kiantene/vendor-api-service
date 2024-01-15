package com.nextgen.gameaggregator.vendor.hacksaw.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ResponseVo implements HttpResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long accountBalance;
    private Integer statusCode;
    private String statusMessage;
    //    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String externalTransactionId;

    @JsonIgnore
    private ResponseCodes responseCodes;

    public ResponseVo() {
        this.setResponseCodes(ResponseCodes.SUCCESS);
    }

    public void setResponseCodes(ResponseCodes responseCodes) {
        this.responseCodes = responseCodes;
        this.statusCode = responseCodes.statusCode;
        this.statusMessage = responseCodes.statusMessage;
    }

    @Override
    public boolean hasError() {
        return !this.responseCodes.equals(ResponseCodes.SUCCESS);
    }

}
