package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
    private Long resultTime;
    private Long vendorSettleTime;
    private WinType resultType;
    private BigDecimal winLoss;
    private BigDecimal vendorWinLoss;
    private BigDecimal effectiveTurnover;

    @Override
    public String getExternalTransactionId() {
        return this.transferId;
    }

    public void setExternalTransactionId(String transferId) {
        this.transferId = transferId;
    }

    @Override
    public String getVendorBetId() {
        return this.transferId;
    }

    public void setVendorBetId(String transferId){
        this.transferId = transferId;
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
        return null;
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
        return this.winLoss;
    }

    public void setWinLoss(BigDecimal winLoss) {
        this.winLoss = winLoss;
    }

    @Override
    public BigDecimal getVendorWinLoss() {
        return this.vendorWinLoss;
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
        return null;
    }

    public void setResultType(WinType resultType) {
        this.resultType = resultType;
    }

    @Override
    public Long getResultTime() {
        return this.resultTime;
    }

    public void setResultTime(Long resultTime) {
        this.resultTime = resultTime;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.vendorSettleTime;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsCancelled() {
        return 0;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    public void setVendorSettleTime(Long vendorSettleTime) {
        this.vendorSettleTime = vendorSettleTime;
    }


}
