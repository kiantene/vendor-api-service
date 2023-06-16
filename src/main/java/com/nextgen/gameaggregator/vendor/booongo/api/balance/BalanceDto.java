package com.nextgen.gameaggregator.vendor.booongo.api.balance;

import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BalanceDto {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(max = 35)
    private String uid;

    @NotBlank
    private String token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 32, max = 32)
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
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+]\\d{2}:\\d{2})$")
    private String c_at;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+]\\d{2}:\\d{2})$")
    private String sent_at;

    @NotNull
    private BalanceArgsDto args;
}
