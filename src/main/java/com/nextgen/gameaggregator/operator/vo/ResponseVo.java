package com.nextgen.gameaggregator.operator.vo;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class ResponseVo {
    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotNull(message = "status can not be blank")
    private ResponseCodes.Status status;

    private String message;
}
