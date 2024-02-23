package com.nextgen.gameaggregator.vendor.bombay.api.credit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreditDto {

    @NotBlank
    private String transaction_uuid;

    @NotBlank
    private String token;

    @NotBlank
    private String request_uuid;

    @NotBlank
    private String reference_transaction_uuid;

    @NotBlank
    private String round;

    @NotNull
    private Integer game_id;

    @NotBlank
    private String currency;

    @NotBlank
    private String offer_id;

    @NotNull
    private Integer amount;
}
