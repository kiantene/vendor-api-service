package com.nextgen.gameaggregator.vendor.cpgame.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends CommonDto implements BetResultData {

    @NotNull
    private MessageDto messageDto;

    public void convertStringToJsonObject(String message) throws JsonProcessingException {
        MessageDto subDto = HttpService.convertJsonToDto(message, MessageDto.class);

        setMessageDto(subDto);
    }

    @Override
    public String getExternalTransactionId() {
        return this.messageDto.getBetInfo().getBetId();
    }

    @Override
    public String getVendorBetId() {
        return this.messageDto.getBetInfo().getBetId();
    }

    @Override
    public String getRoundId() {
        if (!this.messageDto.getBetInfo().getParentBetId().isEmpty()
                && this.messageDto.getBetInfo().getBetAmount().equals(BigDecimal.ZERO)
                && this.messageDto.getBetInfo().getJackpot() == 0) {
            return this.messageDto.getBetInfo().getParentBetId();
        }
        return this.messageDto.getBetInfo().getBetId();
    }

    @Override
    public String getGameId() {
        return this.messageDto.getGameId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.messageDto.getBetInfo().getBetAmount();
    }

    @Override
    public BigDecimal getWinAmount() {
        if (this.messageDto.getBetInfo().getJackpot() == 1) {
            return BigDecimal.ZERO;
        }
        return this.messageDto.getBetInfo().getWinAmount();
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.messageDto.getBetInfo().getWinAmount().
                subtract(this.messageDto.getBetInfo().getBetAmount());
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.messageDto.getBetInfo().getBetAmount();
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
        BigDecimal amount = BigDecimal.ZERO;
        if (this.messageDto.getBetInfo().getJackpot() == 1) {
            amount = messageDto.getBetInfo().getWinAmount();
        }
        return amount;
    }

    @Override
    public Integer getIsFreespin() {
        int status = 0;
        if (this.messageDto.getBetInfo().getBetAmount().equals(BigDecimal.ZERO)
                && this.messageDto.getBetInfo().getJackpot() == 0) {
            status = 1;
        }
        return status;
    }

    @Override
    public BetStatus getBetStatus() {
        if (this.messageDto.getBetInfo().getIsSettled() == 1) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;

    }
}
