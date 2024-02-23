package com.nextgen.gameaggregator.vendor.bombay.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
