package com.nextgen.gameaggregator.vendor.egtdigital.api.bet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.egtdigital.dto.RequestCommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class BetRequest extends RequestCommonDto {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameKey")
    private String gameKey;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transferId")
    private String transferId;

    @Size(max = 255)
    @NotBlank
    @JsonProperty("roundNumber")
    private String roundNumber;

    @NotNull
    @JsonProperty("roundCompleted")
    private Boolean roundCompleted;

    @NotNull
    @Digits(integer = 20, fraction = 0)
    @DecimalMin(value = "0")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("currency")
    private String currency;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("reason")
    private String reason;

}
