package com.nextgen.gameaggregator.vendor.endorphina.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("token")
    private String token;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("sign")
    private String sign;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("id")
    private String id;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameId")
    private String gameId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("currency")
    private String currency;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("player")
    private String player;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("game")
    private String game;

    @NotNull
    @JsonProperty("date")
    private Long date;

    @NotNull
    @Digits(integer = 20, fraction = 3)
    @DecimalMin(value = "0.0")
    @JsonProperty("amount")
    private BigDecimal amount;
}