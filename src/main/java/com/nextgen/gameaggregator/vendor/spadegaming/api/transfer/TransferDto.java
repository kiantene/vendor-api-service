package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.time.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDto implements BetData{
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

    @NotBlank
    @Range(min = 1)
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
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String ticketId;

    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String referenceId;

    private SpecialGameDto specialGame;

    @Size(max = 2048)
    private String refTicketIds;

    @Size(max = 50)
    private String gameFeature;

    @Override
    public String getExternalTransactionId() {
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
    public Long getTimestamp() {
        Instant instant = LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant();
        long epochSecond = instant.getEpochSecond();
        return epochSecond;
    }
}
