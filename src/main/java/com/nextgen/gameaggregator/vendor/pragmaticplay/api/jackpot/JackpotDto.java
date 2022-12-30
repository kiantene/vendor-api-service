package com.nextgen.gameaggregator.vendor.pragmaticplay.api.jackpot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinAction;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JackpotDto implements WinData {

    // Hash code of the request
    @NotBlank
    private String hash;

    // Game Provider id.
    @NotBlank
    private String providerId;

    // Date and time when the transaction is processed on the Pragmatic Play side
    // (Unix epoch time in milliseconds, for example : 1470926696715)
    @Positive
    @NotNull
    private Long timestamp;

    // Identifier of the user within the Casino Operator’s system.
    @NotBlank
    private String userId;

    // Id of the game.
    @NotBlank
    @Size(min = 1, max = 32)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String gameId;

    // Id of the round.
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String roundId;

    // Id of the jackpot.
    @NotBlank
    private String jackpotId;

    // Amount of the bet. Minimum is 0.00.
    @Positive
    @NotNull
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    // Unique reference of this transaction.
    @NotBlank
    @Size(min = 1, max = 32)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String reference;

    // Token of the player from Authenticate response.
    @NotBlank
    private String token;

    @Override
    public String getExternalTransactionId() { return this.reference; }
    @Override
    public String getWinType() { return WalletWinAction.TYPE_JACKPOT; }
}
