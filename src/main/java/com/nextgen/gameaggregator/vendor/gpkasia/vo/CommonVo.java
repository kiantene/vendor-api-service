package com.nextgen.gameaggregator.vendor.gpkasia.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    private Integer code;

    private String msg;

    private DataVo data;

    public void setCodeMsg(Integer code) {
        this.code = code;
        this.msg = ResponseCodes.RESPONSE_DESCRIPTION.get(this.code);
    }

    @Override
    public boolean hasError() {
        if(this.code != 0){
            return true;
        }

        return false;
    }
}
