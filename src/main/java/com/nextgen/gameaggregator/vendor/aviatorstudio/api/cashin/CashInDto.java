package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.aviatorstudio.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class CashInDto extends CommonDto implements BetResultData {

    @NotBlank
    @Size(max = 255)
    String previousTransactionId;


    @Override
    public String getExternalTransactionId() {
        return this.getTransactionId();
    }

    @Override
    public String getVendorBetId() {
        return previousTransactionId;
    }

    @Override
    public String getRoundId() {
        return this.getVendorRoundId();
    }

    @Override
    public String getGameId() {
        return this.getVendorGameId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getAmount();
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.getAmount();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
