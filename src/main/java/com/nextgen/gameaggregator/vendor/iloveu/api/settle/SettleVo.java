package com.nextgen.gameaggregator.vendor.iloveu.api.settle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.iloveu.vo.CommonVo;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettleVo implements HttpResponse {

    public List<CommonVo> transactions;

    @Override
    public boolean hasError() {
        return false;
    }
}
