package com.nextgen.gameaggregator.vendor.alizegames.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class ResponseVo implements HttpResponse {
    private Integer error;      // Response status
    private String message; // Response status short description

    @Override
    public boolean hasError() {
        return false;
    }
}
