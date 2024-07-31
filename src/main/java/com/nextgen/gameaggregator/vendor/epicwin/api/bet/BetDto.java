package com.nextgen.gameaggregator.vendor.epicwin.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.epicwin.constant.Formats;
import com.nextgen.gameaggregator.vendor.epicwin.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.epicwin.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZoneId;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends CommonDto implements BetResultData {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("RoundId")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("BetId")
    private String betId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameCode")
    private String gameCode;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("BetAmount")
    private BigDecimal betAmount;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    @JsonProperty("TranDateTime")
    private String tranDateTime; //placed bet time

    @NotBlank
    @Size(max = 500)
    @JsonProperty("AuthToken")
    private String authToken; //authenticate and validate a player's game session

    @Override
    public String getExternalTransactionId() {
        return this.betId;
    }

    @Override
    public String getVendorBetId() {
        return this.betId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return this.gameCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.betAmount;
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.convertDateTimeStringToTimestamp(this.tranDateTime, Formats.DATE_FORMAT, ZoneId.of(Formats.TIME_ZONE));
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
