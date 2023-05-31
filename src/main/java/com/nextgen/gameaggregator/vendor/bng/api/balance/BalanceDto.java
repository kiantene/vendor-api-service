package com.nextgen.gameaggregator.vendor.bng.api.balance;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BalanceDto {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_\\-.]*$")
    @Size(max = 35)
    private String uid;
    private String token;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_\\-.]*$")
    @Size(min = 32)
    private String session;

    @NotBlank
    private String game_id;

    @NotBlank
    private String game_name;

    @NotBlank
    private String provider_id;

    @NotBlank
    private String provider_name;

    @NotBlank
    private String c_at;

    @NotBlank
    private String sent_at;

    @NotNull
    private BalanceArgsDto args;
}
