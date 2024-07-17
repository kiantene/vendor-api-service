package com.nextgen.gameaggregator.operator.sport.unsettle;

import com.nextgen.gameaggregator.core.WalletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public class UnsettleWalletRequest extends WalletRequest {

    public UnsettleWalletRequest(String traceId) {
        super(traceId);
    }

    public UnsettleWalletRequest(WalletRequest walletRequest) {
        super(walletRequest);
    }

    @Override
    @NotBlank(message = "TraceId cannot be blank")
    public String getTraceId() {
        return super.getTraceId();
    }

    @Override
    @NotBlank(message = "VendorPlayerUsername cannot be blank")
    public String getVendorPlayerUsername() {
        return super.getVendorPlayerUsername();
    }

    @Override
    @NotBlank(message = "ExternalTransactionId cannot be blank")
    public String getExternalTransactionId() {
        return super.getExternalTransactionId();
    }

    @Override
    @NotBlank(message = "VendorBetId cannot be blank")
    public String getVendorBetId() {
        return super.getVendorBetId();
    }

    @Override
    @NotNull(message = "Timestamp cannot be null")
    public Long getTimestamp() {
        return super.getTimestamp();
    }

}
