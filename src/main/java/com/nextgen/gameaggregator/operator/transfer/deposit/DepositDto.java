package com.nextgen.gameaggregator.operator.transfer.deposit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.util.customvalidation.BigDecimalDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DepositDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String referenceId;

    @NotBlank(message = "min 3 and max 20 alphanumeric")
    @Size(min = 3, max = 20 , message = "min 3 and max 20 alphanumeric")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "min 3 and max 20 alphanumeric only") // Only alphanumeric allowed
    private String username;

    @NotBlank( message = "min 3 and max 10  characters")
    @Size(min = 3, max = 10, message = "min 3 and max 10  characters")
    private String currency;

    @JsonDeserialize(using = BigDecimalDeserializer.class) // Use custom deserializer for BigDecimal
    @NotNull(message = "Transfer amount must be positive with 8 decimal places")
    @DecimalMin(value = "0.00000001", inclusive = false, message = "Transfer amount must be positive with 8 decimal places")
    @DecimalMax(value = "999999999999999999.99999999", inclusive = true, message = "Transfer amount exceeds allowed limit")
    private BigDecimal transferAmount;
}
