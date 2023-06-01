package com.nextgen.gameaggregator.vendor.bng.api.login;


import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginDto {

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
    private LoginArgsDto args;
}
