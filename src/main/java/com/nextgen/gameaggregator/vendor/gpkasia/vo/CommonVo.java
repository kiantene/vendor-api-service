package com.nextgen.gameaggregator.vendor.gpkasia.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    private int code;

    private String msg;

    private DataVo data;

    @Override
    public boolean hasError() {
        if(this.code != 0){
            return true;
        }

        return false;
    }
}
