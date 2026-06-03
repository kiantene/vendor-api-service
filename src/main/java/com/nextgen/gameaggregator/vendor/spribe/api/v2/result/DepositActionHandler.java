package com.nextgen.gameaggregator.vendor.spribe.api.v2.result;

import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;

public interface DepositActionHandler {
    boolean supports(String action);
    SuccessResponse handle(BetResultRequest request);
}
