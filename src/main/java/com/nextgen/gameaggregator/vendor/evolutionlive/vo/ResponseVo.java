package com.nextgen.gameaggregator.vendor.evolutionlive.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.evolutionlive.constant.ResponseCode;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    private String status;
    private BigDecimal balance;
    private BigDecimal bonus;
    private String uuid;
    private String sid;

    @JsonIgnore
    private ResponseCode responseCode;

    public ResponseVo() {
        this.setResponseCode(ResponseCode.OK);
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
        this.status = responseCode.status;
    }

    @Override
    public boolean hasError() {
        return !this.responseCode.equals(ResponseCode.OK);
    }

}
