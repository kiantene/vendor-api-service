package com.nextgen.gameaggregator.vendor.evolution.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public void setBalance(BigDecimal balance) {
        this.balance = balance.setScale(2, RoundingMode.DOWN);
    }

    @Override
    public boolean hasError() {
        return !this.responseCode.equals(ResponseCode.OK);
    }

}
