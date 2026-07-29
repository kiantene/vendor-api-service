package com.nextgen.gameaggregator.vendor.topbet.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRequest {

    @NotBlank
    @Size(max = 255)
    private String pid;

    @NotBlank
    @Size(max = 255)
    private String account;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("trans_id")
    private String transId;

    @NotNull
    @Digits(integer = 20, fraction = 2)
    @PositiveOrZero
    private BigDecimal amount;

    @NotNull
    @JsonProperty("app_id")
    private Integer appId;

    @NotBlank
    @Size(max = 255)
    private String action;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("action_id")
    private String actionId;

    @NotNull
    private Long time;

    @NotBlank
    @Size(max = 255)
    private String sign;

    @AssertTrue(message = "Action must be either 'Game Bet' or 'Game ReBet'")
    public boolean isActionValid() {
        if (action == null) {
            return false;
        }
        return action.equalsIgnoreCase("Game Bet") || action.equalsIgnoreCase("Game ReBet");
    }

    @AssertTrue(message = "transId and actionId must not contain spaces")
    public boolean isTransIdAndActionIdValid() {
        return transId != null
                && actionId != null
                && !transId.contains(" ")
                && !actionId.contains(" ");
    }
}
