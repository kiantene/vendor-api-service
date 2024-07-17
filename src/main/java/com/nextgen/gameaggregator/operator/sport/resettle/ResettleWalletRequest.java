package com.nextgen.gameaggregator.operator.sport.resettle;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.enums.BetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ResettleWalletRequest extends WalletRequest {
    public ResettleWalletRequest(String traceId) {
        super(traceId);
    }

    public ResettleWalletRequest(WalletRequest walletRequest) {
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
    @NotBlank(message = "RoundId cannot be blank")
    public String getRoundId() {
        return super.getRoundId();
    }

    @Override
    @NotNull(message = "BetStatus cannot be null")
    public BetStatus getBetStatus() {
        return super.getBetStatus();
    }

    @Override
    @NotNull(message = "WinAmount cannot be null")
    public BigDecimal getWinAmount() {
        return super.getWinAmount();
    }

    @Override
    @NotNull(message = "NewWinAmount cannot be null")
    public BigDecimal getNewWinAmount() {
        return super.getNewWinAmount();
    }
}
