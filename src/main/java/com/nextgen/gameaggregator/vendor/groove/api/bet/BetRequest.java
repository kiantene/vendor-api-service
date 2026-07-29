package com.nextgen.gameaggregator.vendor.groove.api.bet;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BetRequest {

    @NotBlank
    @Size(max = 60)
    private String accountid;

    @NotBlank
    @Size(max = 255)
    private String apiversion;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 22, fraction = 10)
    private BigDecimal betamount;

    @NotBlank
    @Size(max = 255)
    private String device;

    @NotBlank
    @Size(max = 255)
    private String gameid;

    @NotBlank
    @Size(max = 64)
    private String gamesessionid;

    @NotBlank
    @Size(max = 255)
    private String request;

    @NotBlank
    @Size(max = 255)
    private String roundid;

    @NotBlank
    @Size(max = 255)
    private String transactionid;

    @Size(max = 255)
    private String frbid;

    @AssertTrue(message = "roundid must not contain spaces")
    public boolean isRoundIDValid() {
        return roundid != null && !roundid.contains(" ");
    }

    @AssertTrue(message = "transactionid must not contain spaces")
    public boolean isTransactionIDValid() { return transactionid != null && !transactionid.contains(" "); }
}
