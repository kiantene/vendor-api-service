package com.nextgen.gameaggregator.vendor.bombay.api.endround;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EndroundDto {
    @NotNull
    private Integer game_id;

    @NotBlank
    private String round;

    @NotBlank
    private String request_uuid;

    @NotBlank
    private String token;

    @NotBlank
    private String currency;

    @NotBlank
    private String offer_id;
}
