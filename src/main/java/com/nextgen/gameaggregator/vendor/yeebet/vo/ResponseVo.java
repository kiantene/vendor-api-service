package com.nextgen.gameaggregator.vendor.yeebet.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class ResponseVo implements HttpResponse {

    private Integer result;

    private String desc;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double balance;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String orderno;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String serialnumber;

    @Override
    public boolean hasError() {

        if(this.result.equals(0)){
            return false;
        }else{
            return true;
        }

    }
}
