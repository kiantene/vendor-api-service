package com.nextgen.gameaggregator.operator.transactions.detail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDetailDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotBlank(message = "transactionId required")
    @Size(min = 36, max = 36, message = "Invalid transactionId format")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid transactionId format") // Only alphanumeric allowed
    private String transactionId;

}
