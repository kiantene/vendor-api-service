package com.nextgen.gameaggregator.vendor.aviatorstudio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {

    @NotBlank
    String authorization;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    BigDecimal amount;

    @JsonProperty("gameId")
    @NotBlank
    @Size(max = 10)
    String vendorGameId;

    @NotBlank
    @Size(max = 255)
    String transactionId;

    @JsonProperty("roundId")
    @NotBlank
    @Size(max = 255)
    String vendorRoundId;

    @NotBlank
    @Size(max = 5)
    String currency;

    @NotBlank
    @Size(max = 255)
    String sessionId;
}
