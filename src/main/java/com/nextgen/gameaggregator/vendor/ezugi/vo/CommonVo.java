package com.nextgen.gameaggregator.vendor.ezugi.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class CommonVo implements HttpResponse {
    private Integer operatorId;
    private String token;
    private Integer errorCode;
    private String errorDescription;
    private Long timestamp;

    @Override
    public boolean hasError() {
        return false;
    }
}
