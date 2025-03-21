package com.nextgen.gameaggregator.vendor.cpgame.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.cpgame.dto.BetInfoDto;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends CommonDto implements BetResultData {

    @Override
    public String getExternalTransactionId() {
        return super.getMessageDto().getBetInfo().getBetId();
    }

    @Override
    public String getVendorBetId() {
        return super.getMessageDto().getBetInfo().getBetId();
    }

    @Override
    public String getRoundId() {
        BetInfoDto betInfoDto = super.getMessageDto().getBetInfo();

        if (!betInfoDto.getParentBetId().isEmpty()
                && betInfoDto.getBetAmount().compareTo(BigDecimal.ZERO) == 0
                && betInfoDto.getJackpot() == 0) {
            return betInfoDto.getParentBetId();
        }
        return betInfoDto.getBetId();
    }

    @Override
    public String getGameId() {
        return super.getMessageDto().getGameId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return super.getMessageDto().getBetInfo().getBetAmount();
    }

    @Override
    public BigDecimal getWinAmount() {
        BetInfoDto betInfoDto = super.getMessageDto().getBetInfo();

        if (betInfoDto.getJackpot() == 1) {
            return BigDecimal.ZERO;
        }
        return betInfoDto.getWinAmount();
    }

    @Override
    public BigDecimal getWinLoss() {
        BetInfoDto betInfoDto = super.getMessageDto().getBetInfo();

        return betInfoDto.getWinAmount().
                subtract(betInfoDto.getBetAmount());
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return super.getMessageDto().getBetInfo().getBetAmount();
    }

    @Override
    public Long getVendorBetTime() {
        return this.getTime() * 1000;
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
        BetInfoDto betInfoDto = super.getMessageDto().getBetInfo();
        BigDecimal amount = BigDecimal.ZERO;

        if (betInfoDto.getJackpot() == 1) {
            amount = betInfoDto.getWinAmount();
        }
        return amount;
    }

    @Override
    public Integer getIsFreespin() {
        BetInfoDto betInfoDto = super.getMessageDto().getBetInfo();
        int status = 0;

        if (betInfoDto.getBetAmount().equals(BigDecimal.ZERO)
                && betInfoDto.getJackpot() == 0) {
            status = 1;
        }
        return status;
    }

    @Override
    public BetStatus getBetStatus() {
        if (super.getMessageDto().getBetInfo().getIsSettled() == 1) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;

    }
}
