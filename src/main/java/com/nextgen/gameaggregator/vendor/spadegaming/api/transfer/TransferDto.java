package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDto implements BetResultData, RollbackData {
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

    public String getAcctId() {
        return this.acctId.toLowerCase();
    }

    @Override
    public String getExternalTransactionId() {
        return transferId;
    }

    @Override
    public String getVendorBetId() {
        return transferId;
    }

    @Override
    public String getRoundId() {
        return transferId;
    }

    @Override
    public String getGameId() {
        return gameCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public BigDecimal getWinLoss() {
        return getBetAmount().negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return getBetAmount();
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

    @Override
    public String getRollbackId() {
        return this.referenceId;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
