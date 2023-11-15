package com.nextgen.gameaggregator.vendor.iloveu.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.iloveu.constant.ResponseCodes;
import lombok.Data;

@Data
public class CommonVo implements HttpResponse {

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Data")
    private DataVo dataVo;

    public CommonVo () {
        this.setCode(ResponseCodes.SUCCESS);
        DataVo dataVo = new DataVo();
        this.setDataVo(dataVo);
    }

    public void setResponseCode(String responseCode) {
        this.setCode(responseCode);
        this.setDataVo(null);
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
