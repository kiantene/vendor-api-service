package com.nextgen.gameaggregator.operator.sport.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.operator.dto.MultipleBetDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class MultipleBetWalletRequest extends WalletRequest {
    public MultipleBetWalletRequest(String traceId) {
        super(traceId);
    }

    public MultipleBetWalletRequest(WalletRequest walletRequest) {
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

    @Override
    @NotEmpty(message = "BetIds list cannot be empty for multiple bet")
    public List<MultipleBetDto> getBetIds() {
        return super.getBetIds();
    }
}
