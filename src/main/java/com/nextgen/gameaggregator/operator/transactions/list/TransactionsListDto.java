package com.nextgen.gameaggregator.operator.transactions.list;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionsListDto {
    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotNull(message = "long integer number only")
    @Positive(message = "long integer number only")
    @Range(min= 1, max= Long.MAX_VALUE)
    private Long fromTime;

    @NotNull(message = "long integer number only")
    @Positive(message = "long integer number only")
    @Range(min= 1, max= Long.MAX_VALUE)
    private Long toTime;

    @NotNull( message = "numeric number only")
    @Positive( message = "numeric number only")
    @Range(min= 1, max= Integer.MAX_VALUE, message = "numeric number only")
    private Integer pageNo;

    @NotNull(message = "min 1 and max 500 numeric number only")
    @Positive(message = "min 1 and max 500 numeric number only")
    @Range(min= 1, max= 500, message = "min 1 and max 500 numeric number only")
    private Integer pageSize = 500;
}
