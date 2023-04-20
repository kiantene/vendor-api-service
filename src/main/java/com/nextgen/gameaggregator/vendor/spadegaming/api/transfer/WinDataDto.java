package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WinDataDto implements BetResultData {
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
    private BigDecimal vendorWinLoss;
    private BigDecimal effectiveTurnover;

    public BigDecimal setBetAmount() {
        // Bet amount is zero when free spin
        return (getSpecialGame() == null) ? BigDecimal.ZERO : null;
    }
    
    public String getAcctId() {
        return this.acctId.toLowerCase();
    }

    @Override
    public String getExternalTransactionId() {
        return this.transferId;
    }

    @Override
    public String getVendorBetId() {
        return this.referenceId;
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

    @Override
    public BigDecimal getWinLoss() {
        return this.amount;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.effectiveTurnover;
    }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.valueOf(0);
    }

//    @Override
//    public ResultType getResultType() {
//        return this.amount.compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.LOSE;
//    }

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
