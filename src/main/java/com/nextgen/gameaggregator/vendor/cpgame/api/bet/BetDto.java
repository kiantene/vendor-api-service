package com.nextgen.gameaggregator.vendor.cpgame.api.bet;

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
public class BetDto implements BetResultData {

    @Autowired
    private HttpService httpService;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String appid;

    @NotNull
    private Long time;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String token;

    @NotBlank
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$")
    private String message;

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
        return new BigDecimal(this.messageDto.getBetInfo().getBetAmount());
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
        return new BigDecimal(this.messageDto.getBetInfo().getBetAmount());
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
        int status = 0;
        if (this.messageDto.getBetInfo().getBetAmount() == 0) {
            status = 1;
        }
        return status;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
