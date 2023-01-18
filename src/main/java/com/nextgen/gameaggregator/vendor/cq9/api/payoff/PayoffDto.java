package com.nextgen.gameaggregator.vendor.cq9.api.payoff;

import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class PayoffDto {
    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String account;

    @NotBlank
    private String eventTime;

    @NotNull
    @Positive
//    @Digits(integer = 12, fraction = 4)
    private BigDecimal amount;

    @NotBlank
    private String mtcode;

    private String promoid;
    private String remark;
}
