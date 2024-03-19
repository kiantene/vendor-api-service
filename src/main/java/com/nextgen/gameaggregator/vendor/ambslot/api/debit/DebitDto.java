package com.nextgen.gameaggregator.vendor.ambslot.api.debit;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ambslot.service.VendorService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitDto implements BetResultData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String username;

    @NotBlank
    private String agent;

    @NotBlank
    private String game;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String gameId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String roundId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String transactionId;

    @NotNull
    @DecimalMin(value = "0")
    private Double amount;

    @NotNull
    @DecimalMin(value = "0")
    private Double turnover;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z]*$")
    private String currency;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$")
    private String timestamp;

    @NotBlank
    @Pattern(regexp = "^slot$")
    private String type;

    @NotNull
    private Boolean isEndRound;

    @NotNull
    private Boolean featureBuy;

    @NotNull
    @DecimalMin(value = "0")
    private Double roundAmount;

    @NotNull
    private Boolean isGamble;

    @NotNull
    private Boolean isFreespin;

    @NotNull
    private Boolean isBuyFeature;

    @Override
    public String getExternalTransactionId() {
        return this.getTransactionId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTransactionId();
    }

    @Override
    @JsonIgnore
    public String getRoundId() {
        return this.getTransactionId();
    }

    @Override
    @JsonIgnore
    public String getGameId() {
        return this.gameId;
    }

    @Override
    public BigDecimal getBetAmount() {
        return new BigDecimal(this.getAmount());
    }

    @Override
    public BigDecimal getWinAmount() {
        if(this.isEndRound.equals(true)){
            return BigDecimal.ZERO;
        }else{
            return null;
        }
    }

    @Override
    public BigDecimal getWinLoss() {
        if(this.isEndRound.equals(true)){
            return BigDecimal.ZERO;
        }else{
            return null;
        }
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return new BigDecimal(this.getTurnover());
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.convertDateTimeToUnix(this.getTimestamp());
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        if(this.isEndRound.equals(true)){
            return VendorService.convertDateTimeToUnix(this.getTimestamp());
        }else{
            return null;
        }
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        int status = 0;

        if(this.isFreespin.equals(true)){
            status = 1;
        }

        return status;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
