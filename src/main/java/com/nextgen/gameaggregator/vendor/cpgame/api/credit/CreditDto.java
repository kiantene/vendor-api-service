package com.nextgen.gameaggregator.vendor.cpgame.api.credit;

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
public class CreditDto extends CommonDto implements BetResultData {

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
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.messageDto.getBetInfo().getWinAmount();
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
