package com.nextgen.gameaggregator.vendor.groove.api.rollback;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RollbackRequest {

    @NotBlank
    @Size(max = 60)
    private String accountid;

    @NotBlank
    private String apiversion;

    @NotBlank
    private String device;

    @NotBlank
    private String gameid;

    @NotBlank
    @Size(max = 64)
    private String gamesessionid;

    @NotBlank
    private String request;

    @NotBlank
    @Size(max = 255)
    private String transactionid;

    @Digits(integer = 22, fraction = 10)
    private BigDecimal rollbackamount;

    @Size(max = 255)
    private String roundid;

    @AssertTrue(message = "transactionid must not contain spaces")
    public boolean isTransactionIDValid() { return transactionid != null && !transactionid.contains(" "); }
}
