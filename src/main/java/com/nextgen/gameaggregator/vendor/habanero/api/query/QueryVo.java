package com.nextgen.gameaggregator.vendor.habanero.api.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryVo implements HttpResponse {

    @JsonProperty("fundtransferresponse")
    private FundTransferResponseVo fundTransferResponseVo;

    @Override
    public boolean hasError() {
        return false;
    }
}
