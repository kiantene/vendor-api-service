package com.nextgen.gameaggregator.vendor.bombay.api.rollback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RollbackDto {
    @NotBlank
    private String transaction_uuid;

    @NotBlank
    private String token;

    @NotBlank
    private String request_uuid;

    @NotBlank
    private String reference_transaction_uuid;

    @NotNull
    private Integer game_id;
}
