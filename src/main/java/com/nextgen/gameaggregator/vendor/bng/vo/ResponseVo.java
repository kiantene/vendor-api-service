package com.nextgen.gameaggregator.vendor.bng.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class ResponseVo<T> implements HttpResponse {
    private T data;
    private StatusVo status;

    @Override
    public boolean hasError() {
        return this.status != null && !this.status.getCode().equals("0");
    }
}
