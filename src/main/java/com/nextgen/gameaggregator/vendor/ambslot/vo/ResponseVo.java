package com.nextgen.gameaggregator.vendor.ambslot.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class ResponseVo implements HttpResponse {
    private StatusVo status;

    @Override
    public boolean hasError() {
        boolean status = false;

        if(!this.getStatus().getCode().equals(0)){
            status = true;
        }

        return status;
    }
}
