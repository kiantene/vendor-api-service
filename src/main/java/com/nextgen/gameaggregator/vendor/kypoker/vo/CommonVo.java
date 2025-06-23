package com.nextgen.gameaggregator.vendor.kypoker.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class CommonVo implements HttpResponse {
    private String m;

    private Integer s;

    public ResponseObjectDto d;

    @Override
    public boolean hasError() {
        return false;
    }
}


