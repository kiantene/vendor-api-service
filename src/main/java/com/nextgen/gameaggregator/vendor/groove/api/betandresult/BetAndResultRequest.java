package com.nextgen.gameaggregator.vendor.groove.api.betandresult;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetAndResultRequest {

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
    private String gamestatus;

    @NotBlank
    @Size(max = 255)
    private String request;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 22, fraction = 10)
    private BigDecimal result;

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
