package com.nextgen.gameaggregator.vendor.lucky365.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetResultRequest {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("SN")
    private String sn;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("ID")
    private String id;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("Method")
    private String method;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("LoginId")
    private String loginId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("Signature")
    private String signature;

    @NotNull
    @Digits(integer = 18, fraction = 4)
    @DecimalMin(value = "0.0")
    @JsonProperty("TotalWin")
    private BigDecimal totalWin;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("OrderCode")
    private String orderCode;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("ActionDate")
    private String actionDate;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameName")
    private String gameName;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameCode")
    private String gameCode;

    @NotNull
    @JsonProperty("Mode")
    private Integer mode;

    @NotNull
    @JsonProperty("GameStatus")
    private Integer gameStatus;

    @NotNull
    @Valid
    @JsonProperty("Bet")
    private Bet bet;


    @Data
    @NotNull
    public static class Bet {
        @NotNull
        @Digits(integer = 18, fraction = 2)
        @DecimalMin(value = "0.0")
        @JsonProperty("TotalBet")
        private BigDecimal totalBet;

    }

}