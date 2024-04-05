package com.nextgen.gameaggregator.vendor.gpkasia.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class ResponseVo implements HttpResponse {

    private Integer code;

    private String msg;

    private DataVo data;

//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    private Double balance;

    @Override
    public boolean hasError() {
        if(!code.equals(0)){
            return true;
        }

        return false;
    }
}
