package com.nextgen.gameaggregator.vendor.ygg.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evolution.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WagerDto implements BetResultData {

    @NotBlank
    @Size(max = 255)
    private String org;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("sessiontoken")
    private String sessionToken;

    @NotBlank
    @Size(max = 50)
    @JsonProperty("playerid")
    private String playerId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String reference;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @JsonProperty("subreference")
    private String subReference;

    private String cat5;

    private String tag3;

    @Override
    public String getExternalTransactionId() {
        return this.reference;
    }

    @Override
    public String getVendorBetId() {
        return this.subReference;
    }

    @Override
    public String getRoundId() {
        return this.reference;
    }

    @Override
    public String getGameId() {
        return this.cat5;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.amount;
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
        return this.amount;
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.getTimestamp();
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
