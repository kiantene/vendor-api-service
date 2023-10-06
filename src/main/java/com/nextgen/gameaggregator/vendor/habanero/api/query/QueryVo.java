package com.nextgen.gameaggregator.vendor.habanero.api.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryVo implements HttpResponse {

    @JsonProperty("fundtransferresponse")
    private FundTransferResponseVo fundTransferResponseVo;

    public QueryVo() {
        this.setFundTransferResponseVo(new FundTransferResponseVo());
    }

    public void setResponseCode(ResponseCodes responseCode) {
        this.getFundTransferResponseVo().getStatusVo().setSuccess(responseCode.success);
        this.getFundTransferResponseVo().getStatusVo().setRetryStatus(responseCode.retryStatus);
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
