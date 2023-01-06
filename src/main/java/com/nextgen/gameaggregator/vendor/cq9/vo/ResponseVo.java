package com.nextgen.gameaggregator.vendor.cq9.vo;

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
