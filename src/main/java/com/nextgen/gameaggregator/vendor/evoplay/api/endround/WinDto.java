package com.nextgen.gameaggregator.vendor.evoplay.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WinDto extends CallbackDto implements BetResultData {

    @Override
    public String getExternalTransactionId() {
        return this.getData().getRound_id();
    }

    @Override
    public String getVendorBetId() {
        return this.getData().getRound_id();
    }

    @Override
    public String getRoundId() {
        return this.getData().getRound_id();
    }

    @Override
    public String getGameId() {
        return this.getData().getDetailsDto().getGame().getGame_id();
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getData().getAmount();
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.getBetAmount();
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.generateTimestamp();
    }

    @Override
    public Long getResultTime() {
        return VendorService.generateTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.generateTimestamp();
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
        if (this.getData().getFinal_action().equals(1)) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}
