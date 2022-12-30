package com.nextgen.gameaggregator.vendor.pragmaticplay.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultDto implements BetData {

    // Hash code of the request
    @NotBlank
    private String hash;

    // Identifier of the user within the Casino Operator’s system.
    @NotBlank
    private String userId;

    // Id of the game.
    @NotBlank
    @Size(min = 1, max = 32)
    private String gameId;

    // Id of the round.
    @NotBlank
    @Size(max = 100)
    private String roundId;

    // Amount of the bet. Minimum is 0.00.
    @Positive
    @NotNull
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    // Unique reference of this transaction.
    @NotBlank
    @Size(min = 1, max = 32)
    private String reference;

    // Game Provider id.
    @NotBlank
    private String providerId;

    // Date and time when the transaction is processed on the Pragmatic Play side
    // (Unix epoch time in milliseconds, for example : 1470926696715)
    @Positive
    @NotNull
    private Long timestamp;

    // Additional information about the current game round.
    @NotBlank
    @Size(max = 4000)
    private String roundDetails;

    // Token of the player from Authenticate response.
    @NotBlank
    private String token;

    @Override
    public String getExternalTransactionId() { return this.reference; }
}
