package com.nextgen.gameaggregator.operator.sport.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BetWalletRequest extends WalletRequest {
    public BetWalletRequest(String traceId) {
        super(traceId);
    }

    public BetWalletRequest(WalletRequest walletRequest) {
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
    @NotNull(message = "BetAmount cannot be null")
    public BigDecimal getBetAmount() {
        return super.getBetAmount();
    }

}
