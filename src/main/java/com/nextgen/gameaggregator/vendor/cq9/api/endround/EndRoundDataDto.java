package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDataDto {
    @NotBlank
    @Size(min = 1, max = 70)
    private String mtcode;
    @NotNull
    @Positive
//    @Digits(integer = 16, fraction = 4)
    private BigDecimal amount;
    @NotBlank
    private String eventtime;
}
