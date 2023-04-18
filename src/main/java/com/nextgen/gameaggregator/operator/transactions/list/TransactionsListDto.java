package com.nextgen.gameaggregator.operator.transactions.list;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionsListDto {
    @NotBlank
    @Size(min = 36, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric allowed
    private String traceId;

    @NotNull
    @Positive
    @Range(min= 1, max= Long.MAX_VALUE)
    private Long fromTime;

    @NotNull
    @Positive
    @Range(min= 1, max= Long.MAX_VALUE)
    private Long toTime;

    @NotNull
    @Positive
    @Range(min= 1, max= Integer.MAX_VALUE)
    private Integer pageNo;

    @NotNull
    @Positive
    @Range(min= 1, max= 500)
    private Integer pageSize = 500;
}
