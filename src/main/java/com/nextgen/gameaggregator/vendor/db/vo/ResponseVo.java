package com.nextgen.gameaggregator.vendor.db.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.db.constant.ResponseCodes;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    private BigInteger code;

    private String msg;

    private Object data;

    // set a method function to assign value
    public void setResponseCode(Integer code) {

        this.code = BigInteger.valueOf(code);
        this.msg = ResponseCodes.RESPONSE_DESCRIPTION.get(code);
    }


    @Override
    public boolean hasError() {
        boolean status = true;

        // check if the code is not 1000
        if (!this.code.equals(BigInteger.valueOf(ResponseCodes.SUCCESS))) {
            status = false;
        }

        return status;
    }
}
