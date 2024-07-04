package com.nextgen.gameaggregator.vendor.cpgame.api.credit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditDto implements BetResultData {
    @Autowired
    private HttpService httpService;

    @NotNull
    private Long time;

    @NotBlank
    private String appid;

    @NotBlank
    private String message;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String token;

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
        String roundid = this.getMessageDto().getBetInfo().getRoundId();

        if (roundid.equals("") || roundid == null) {
            roundid = this.getMessageDto().getBetInfo().getBetId();
        }

        return roundid;
    }

    @Override
    public String getGameId() {
        return this.getMessageDto().getGameId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return new BigDecimal(this.messageDto.getBetInfo().getWinAmount());
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
        return this.time * 1000;
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return this.time * 1000;
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
