package com.nextgen.gameaggregator.vendor.gpkpushgaming.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    private Integer code;

    private String msg;

    private DataVo data;

    public void setCodeMsg(Integer code) {
        this.code = code;
        this.msg = ResponseCodes.fromCode(this.code).message;
    }

    @Override
    public boolean hasError() {
        return this.code != 0;
    }
}
