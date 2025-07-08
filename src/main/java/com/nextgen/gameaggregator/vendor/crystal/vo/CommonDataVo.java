package com.nextgen.gameaggregator.vendor.crystal.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class CommonDataVo implements HttpResponse {
    private CommonVo data = new CommonVo();
    private ErrorVo error;

    @Override
    public boolean hasError() {
        return error != null;
    }
}