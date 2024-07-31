package com.nextgen.gameaggregator.vendor.epicwin.api.endround;

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
public class SettleDto extends CommonDto implements BetResultData {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("ResultId")
    private String resultId; //for remapping settle bet

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

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("Payout")
    private BigDecimal payout;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("WinLose")
    private BigDecimal winLose; //win lose amount (payout - turnover)

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    @JsonProperty("TranDateTime")
    private String tranDateTime; //settled time

    @NotNull
    @JsonProperty("RoundType")
    private Integer roundType; //0- Normal Round, 1- Buy FreeSpin/Feature Round

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
        return this.payout;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.payout.subtract(this.betAmount);
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
        return VendorService.convertDateTimeStringToTimestamp(this.tranDateTime, Formats.DATE_FORMAT, ZoneId.of(Formats.TIME_ZONE));
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.convertDateTimeStringToTimestamp(this.tranDateTime, Formats.DATE_FORMAT, ZoneId.of(Formats.TIME_ZONE));
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        Integer status = 0;

        if (this.roundType.equals(1)) {
            status = 1;
        }
        return status;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
