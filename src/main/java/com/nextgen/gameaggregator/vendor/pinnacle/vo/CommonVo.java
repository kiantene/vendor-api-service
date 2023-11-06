package com.nextgen.gameaggregator.vendor.pinnacle.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;

import lombok.Data;

@Data
public class CommonVo implements HttpResponse {
    private ResultVo Result;
    private ResponseCode ErrorCode;
    private String Timestamp;

    @Override
    public boolean hasError() {
        return !this.ErrorCode.equals(ResponseCode.SUCCESS);
    }
}
