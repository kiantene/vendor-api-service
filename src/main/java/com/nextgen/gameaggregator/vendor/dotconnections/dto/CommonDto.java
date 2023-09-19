package com.nextgen.gameaggregator.vendor.dotconnections.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {

    @NotBlank
    @Size(max = 7)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String brandId;

    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Z0-9]*$")
    public String sign;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 3, max = 20)
    public String brandUid;

    @NotBlank
    @Size(min = 3, max = 4)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    public Long getCurrentTimeStamp() {
        return Instant.now().toEpochMilli();
    }
}
