package com.nextgen.gameaggregator.vendor.cpgame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {
    @NotBlank
    private String appid;

    @NotNull
    @PositiveOrZero(message = "negative number")
    @Digits(integer = 10, fraction = 0)
    private Long time;

    @NotBlank
    @Size(max = 1000)
    private String token;

    @NotBlank
    private String message;
}
