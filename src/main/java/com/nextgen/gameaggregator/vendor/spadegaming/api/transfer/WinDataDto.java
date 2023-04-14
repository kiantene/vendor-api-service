package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.nextgen.gameaggregator.enums.BetStatus;
import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WinDataDto implements UnsettledResultSettledData {
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String transferId;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String acctId;

    @NotBlank
    @Size(max = 3)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String currency;

    @NotNull
    @Range(min = 0)
    private BigDecimal amount;

    @NotNull
    private Integer type;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String channel;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String gameCode;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String merchantCode;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String serialNo;

    @Size(max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String ticketId;

    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String referenceId;

    private SpecialGameDto specialGame;

    @Size(max = 2048)
    private List<String> refTicketIds;

    @Size(max = 50)
    private String gameFeature;
    
    private String betId;
    private WinType resultType;
    private BigDecimal betAmount;
    private BigDecimal vendorWinLoss;
    private BigDecimal effectiveTurnover;

    public String getAcctId() {
        return this.acctId.toLowerCase();
    }

    @Override
    public String getExternalTransactionId() {
        return this.transferId;
    }

    public void setExternalTransactionId(String transferId) {
        this.transferId = transferId;
    }

    @Override
    public String getVendorBetId() {
        return this.referenceId;
    }

    public void setVendorBetId(String referenceId){
        this.referenceId = referenceId;
    }

    @Override
    public String getRoundId() {
        return this.referenceId;
    }

    @Override
    public String getGameId() {
        return this.gameCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(BigDecimal betAmount) {
        this.betAmount = betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amount;
    }

    public void setWinAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.amount;
    }

    public void setWinLoss(BigDecimal amount) {
        this.amount = amount;
    }

    public void setVendorWinLoss(BigDecimal vendorWinLoss) {
        this.vendorWinLoss = vendorWinLoss;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.effectiveTurnover;
    }

    public void setEffectiveTurnover(BigDecimal effectiveTurnover) {
        this.effectiveTurnover = effectiveTurnover;
    }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public WinType getResultType() {
        return this.resultType;
    }

    @Override
    public Long getVendorBetTime() {
        return getTimestamp();
    }

    @Override
    public Long getResultTime() {
        return getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return getTimestamp();
    }

    public void setResultType(WinType resultType) {
        this.resultType = resultType;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }

    public Long getTimestamp() {
        Instant instant = LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant();
        long epochSecond = instant.getEpochSecond();
        return epochSecond;
    }
}
