package com.nextgen.gameaggregator.vendor.saba.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneralVo implements HttpResponse {
    protected String status;
    protected String msg;
    protected BigDecimal balance;

    @Override
    public boolean hasError() {
        return !this.status.equals("0");
    }

    public void setResponseCode(ResponseCode responseCode){
        this.status = responseCode.status;
        this.msg = responseCode.message;
    }
}
