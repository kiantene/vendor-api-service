package com.nextgen.gameaggregator.vendor.bombay.api.balance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BalanceDto {
    @NotBlank
    private String token;

    @NotBlank
    private String request_uuid;

    @NotNull
    private Integer game_id;
}
