package com.nextgen.gameaggregator.vendor.epicwin.api.jackpot;

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
public class JackpotDto extends CommonDto implements BetResultData {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("TranId")
    private String tranId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("JackpotId")
    private String jackpotId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("Payout")
    private BigDecimal payout;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    @JsonProperty("TranDateTime")
    private String tranDateTime;

    @Override
    public String getExternalTransactionId() {
        return this.tranId;
    }

    @Override
    public String getVendorBetId() {
        return this.tranId;
    }

    @Override
    public String getRoundId() {
        return this.tranId;
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.payout;
    }

    @Override
    public BigDecimal getWinLoss() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.ZERO;
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
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
