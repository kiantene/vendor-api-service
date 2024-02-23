package com.nextgen.gameaggregator.vendor.bombay.api.debit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DebitDto {
    @NotBlank
    private String transaction_uuid;

    @NotBlank
    private String token;

    @NotBlank
    private String round;

    @NotBlank
    private String request_uuid;

    @NotNull
    private Integer game_id;

    @NotBlank
    private String currency;

    @NotBlank
    private String offer_id;

    @NotBlank
    private String bet;

    @NotBlank
    private Integer amount;
}
