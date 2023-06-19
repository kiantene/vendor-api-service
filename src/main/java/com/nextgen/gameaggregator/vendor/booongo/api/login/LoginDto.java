package com.nextgen.gameaggregator.vendor.booongo.api.login;


import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.booongo.constant.ResponseCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDto {

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    private String name;

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.GAME_NOT_ALLOWED)
    @Size(max = 35, message = ResponseCodes.GAME_NOT_ALLOWED)
    private String uid;

    @NotBlank
    private String token;

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 32 , max = 32, message = ResponseCodes.GAME_NOT_ALLOWED)
    private String session;

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    private String game_id;

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    private String game_name;

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    private String provider_id;

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    private String provider_name;

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    // check time format is ISO8061
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})$", message = ResponseCodes.GAME_NOT_ALLOWED)
    private String c_at;

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    // check time format is ISO8061
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})$", message = ResponseCodes.GAME_NOT_ALLOWED)
    private String sent_at;

    @NotNull(message = ResponseCodes.GAME_NOT_ALLOWED)
    private LoginArgsDto args;
}
