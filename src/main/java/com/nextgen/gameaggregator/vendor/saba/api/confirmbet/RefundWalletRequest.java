package com.nextgen.gameaggregator.vendor.saba.api.confirmbet;

import com.nextgen.gameaggregator.core.WalletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class RefundWalletRequest extends WalletRequest {

    @NotBlank
    protected String operatorUsername;

    public RefundWalletRequest(String traceId) {
        super(traceId);
    }
}
