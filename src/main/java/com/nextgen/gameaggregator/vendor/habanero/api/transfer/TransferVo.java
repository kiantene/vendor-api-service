package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferVo implements HttpResponse {

    @JsonProperty("fundtransferresponse")
    private FundTransferResponseVo fundTransferResponseVo;

    public TransferVo() {
        this.setFundTransferResponseVo(new FundTransferResponseVo());
    }

    public void setResponseCode(ResponseCodes responseCode) {
        this.getFundTransferResponseVo().getStatusVo().setSuccess(responseCode.success);
        this.getFundTransferResponseVo().getStatusVo().setAuthError(responseCode.authError);
        this.getFundTransferResponseVo().getStatusVo().setNoFunds(responseCode.noFunds);
        this.getFundTransferResponseVo().getStatusVo().setRefundStatus(responseCode.refundStatus);
        this.getFundTransferResponseVo().getStatusVo().setRetryStatus(responseCode.retryStatus);
        this.getFundTransferResponseVo().getStatusVo().setMessage(responseCode.message);
    }

    public void setDebitNCreditMessage() {
        this.getFundTransferResponseVo().getStatusVo().setSuccessDebit(false);
        this.getFundTransferResponseVo().getStatusVo().setSuccessCredit(false);
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
