package com.nextgen.gameaggregator.vendor.advantplay.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.advantplay.dto.BetSettleRefundDto;
import com.nextgen.gameaggregator.vendor.advantplay.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class PlaceBetDto extends BetSettleRefundDto implements BetResultData {

    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 4)
    private BigDecimal stake;
    @NotBlank
    @Size(min = 1, max = 50)
//    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}[+-]\\d{2}:\\d{2}")
    private String betTime;
    private String ip;

    @Override
    public String getExternalTransactionId() {
        return this.getGameRoundId();
    }

    @Override
    public String getVendorBetId() {
        return this.getGameRoundId();
    }

    @Override
    public String getRoundId() {
        return this.getGameRoundId();
    }

    @Override
    public String getGameId() {
        return this.getGameCode();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getStake();
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
        return this.getStake();
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.dateTimeConvert(this.getBetTime());
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
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
