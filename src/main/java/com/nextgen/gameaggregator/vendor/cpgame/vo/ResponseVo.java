package com.nextgen.gameaggregator.vendor.cpgame.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.cpgame.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {

    private Integer code;

    private String msg;

    private DataVo data;

    public void setCodeMsg(Integer code) {
        this.code = code;
        this.msg = ResponseCodes.RESPONSE_DESCRIPTION.get(this.code);
    }

    @Override
    public boolean hasError() {
        if(!this.code.equals(0)){
            return true;
        }else{
            return false;
        }

    }
}
