package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.bet.SportMultipleBetData;
import com.nextgen.gameaggregator.operator.sport.bet.SportMultipleBetIdsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Formats;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class MultipleBetDto extends ActionsWagerInfoDto implements SportMultipleBetData {
    private String externalTransactionId;

    @Override
    public String getVendorBetId() {
        return this.getWagerId().toString();
    }

    @Override
    public String getRoundId() {
        return this.getWagerId().toString();
    }

    @Override
    public String getGameId() {
        return this.getSportId().toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getStake();
    }

    @Override
    public BigDecimal getNewBetAmount() {
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
        return VendorService.convertDateTimeStringToTimestamp(this.getTransactionDate(), Formats.DATE_TIME_FORMAT_T_SEPARATOR, Formats.GMT_MINUS_FOUR);
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
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }

    @Override
    public Integer getBetType() {
        return this.getType().equalsIgnoreCase("PARLAY") ? BetType.PARLAY_BET.code : BetType.NORMAL_BET.code;
    }

    @Override
    public List<SportMultipleBetIdsDto> getSportMultipleBetIdsDtoList() {
        int wagerNum = this.getWagerNum();
        BigDecimal stake = this.getStake();
        long wagerId = this.getWagerId();

        List<SportMultipleBetIdsDto> sportMultipleBetIdsDtoList = new ArrayList<>(wagerNum);
        BigDecimal betAmount = stake.divide(BigDecimal.valueOf(wagerNum), RoundingMode.UNNECESSARY);

        for (int i = 1; i <= wagerNum; i++) {
            SportMultipleBetIdsDto sportMultipleBetIdsDto = new SportMultipleBetIdsDto();
            sportMultipleBetIdsDto.setBetId(UUID.randomUUID().toString());
            sportMultipleBetIdsDto.setVendorBetId(wagerId + "_" + i);
            sportMultipleBetIdsDto.setBetAmount(betAmount);
            sportMultipleBetIdsDtoList.add(sportMultipleBetIdsDto);
        }

        return sportMultipleBetIdsDtoList;
    }
}
