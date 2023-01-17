package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResettleDto {
    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String account;

    @NotBlank
    private String eventTime;

    @NotBlank
    @Size(min = 1, max = 36)
    private String gamehall;

    @NotBlank
    @Size(min = 1, max = 36)
    private String gamecode;

    @NotBlank
    @Size(min = 1, max = 30)
    private String roundid;

    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 4)
    private BigDecimal amount;

    @NotBlank
    @Size(min = 1, max = 70)
    private String mtcode;
}
