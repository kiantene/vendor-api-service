package com.nextgen.gameaggregator.custodianseamless.operator.getsingletransaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetSingleTransactionDto {

    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String referenceId;

    @NotBlank(message = "min 3 and max 40 alphanumeric")
    @Size(min = 3, max = 40, message = "min 3 and max 40 alphanumeric")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX, message = "min 3 and max 40 alphanumeric only")
    // allow UUID format & "-" & "_"
    private String username;

    @NotBlank(message = "min 3 and max 10  characters")
    @Size(min = 3, max = 10, message = "min 3 and max 10  characters")
    private String currency;
}
