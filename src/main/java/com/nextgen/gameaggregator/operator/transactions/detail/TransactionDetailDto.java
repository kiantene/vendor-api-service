package com.nextgen.gameaggregator.operator.transactions.detail;

import com.nextgen.gameaggregator.util.ValidationUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class TransactionDetailDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotBlank(message = "transactionId required")
    @Size(min = 36, max = 36, message = "Invalid transactionId format")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid transactionId format") // Only alphanumeric allowed
    private String transactionId;

    @NotBlank(message = "min 3 and max 50 alphanumeric")
    @Size(min = 3, max = 50, message = "min 3 and max 50 alphanumeric")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "min 3 and max 50 alphanumeric") // Only alphanumeric allowed
    private String gameCode;

    @NotBlank(message = "2 alphanumeric")
    @Size(min = 2, max = 2, message = " 2 alphanumeric only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "2 alphanumeric only") // Only alphanumeric allowed
    private String language;

}
