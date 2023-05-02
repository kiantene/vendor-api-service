package com.nextgen.gameaggregator.vendor.mg.api.updateBalance;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.mg.constant.DeviceType;
import com.nextgen.gameaggregator.vendor.mg.constant.EventType;
import com.nextgen.gameaggregator.vendor.mg.constant.TxnType;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateBalanceDto implements BetResultData {
    @NotNull
    private TxnType txnType;

    @NotNull
    private EventType txnEventType;

    @NotBlank
    @Size(max = 50)
    private String playerId;
    
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotBlank
    @Size(max = 256)
    private String txnId;

    @NotBlank
    @Size(max = 50)
    private String contentCode;

    @Size(max = 256)
    private String betId;

    @Size(max = 256)
    private String roundId;

    private String metaData;

    private DeviceType deviceType;

    private String platformType;

    @NotNull
    private Boolean completed;

    @Size(max = 50)
    private String channel;

    @NotNull
    private Long creationTimeMs;

    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_,~().!\\*'\\:@;-]*$")
    private String extOperatorToken;

    @Override
    public String getExternalTransactionId() {
        return betId;
    }

    @Override
    public String getVendorBetId() {
        return betId;
    }

    @Override
    public String getGameId() {
        return contentCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return creationTimeMs;
    }

    @Override
    public Long getResultTime() {
        return creationTimeMs;
    }

    @Override
    public Long getVendorSettleTime() {
        return creationTimeMs;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
