package com.nextgen.gameaggregator.operator.transactions.v2.detail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDetailDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotBlank(message = "betId required")
    @Size(min = 36, max = 36, message = "Invalid betId format")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid transactionId format") // Only alphanumeric allowed
    private String betId;

    @NotBlank(message = "2 alphanumeric")
    @Size(min = 2, max = 2, message = " 2 alphanumeric only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "2 alphanumeric only") // Only alphanumeric allowed
    private String displayLanguage = "en";


    @NotNull(message = "long integer number only")
    @Positive(message = "long integer number only")
    @Range(min= 1659282428477L, max= Long.MAX_VALUE, message = "long integer number only")
    private Long fromTime;

    @NotNull(message = "long integer number only")
    @Positive(message = "long integer number only")
    @Range(min= 1659282428477L, max= Long.MAX_VALUE, message = "long integer number only")
    private Long toTime;

}
