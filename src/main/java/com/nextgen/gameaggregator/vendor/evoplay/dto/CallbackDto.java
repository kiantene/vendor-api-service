package com.nextgen.gameaggregator.vendor.evoplay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackDto extends BasicDto {
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String token;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String callback_id;

    @NotBlank
    @Size(max = 50)
    private String name;

    private DataDto data;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String signature;
}
