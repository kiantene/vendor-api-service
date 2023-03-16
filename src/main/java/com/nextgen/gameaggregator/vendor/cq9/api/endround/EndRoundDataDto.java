package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDataDto {
    @NotBlank
    @Size(min = 1, max = 70)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    private String mtcode;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 12, fraction = 10)
    private BigDecimal amount;

    @NotBlank
    private String eventtime;

    public Long getTimestamp(){
        Instant instant = Instant.parse(this.getEventtime());
        return instant.getEpochSecond();
    }
}
