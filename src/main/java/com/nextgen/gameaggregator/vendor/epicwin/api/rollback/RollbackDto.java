package com.nextgen.gameaggregator.vendor.epicwin.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.epicwin.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto extends CommonDto implements RollbackData {
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
    private String tranDateTime;

    @Override
    public String getRollbackId() {
        return this.betId;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
