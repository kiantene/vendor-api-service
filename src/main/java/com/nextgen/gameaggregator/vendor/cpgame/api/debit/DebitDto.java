package com.nextgen.gameaggregator.vendor.cpgame.api.debit;

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
public class DebitDto extends CommonDto implements BetResultData {

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
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
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
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        int status = 0;
        if (this.messageDto.getBetInfo().getBetAmount().equals(BigDecimal.ZERO)) {
            status = 1;
        }
        return status;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
