package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoundPayoutTransactionDto {

    @NotBlank
    @Pattern(regexp = "[a-zA-Z]+")
    @Size(max =24)
    public String type;

    @NotNull
    @Digits(fraction=2, integer=32)
    @DecimalMax(value = "100000000000000000000000000000000.00", inclusive = false)
    @DecimalMin(value = "-100000000000000000000000000000000.00", inclusive = false)
    public BigDecimal amount;

    @NotBlank
    @DateTimeFormat(pattern = "yyyy-MM-ddTHH:mm:ss.SSSZ")
    public String timestamp;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(max =24)
    public String id;

    @Size(max =50)
    @Pattern(regexp = "^[a-zA-Z0-9_-]*$")
    public String info;

    @NotNull
    public Boolean isEnd;

    public Long getTimestamp() {
        Instant instant = Instant.parse(this.timestamp);
        return instant.getEpochSecond();
    }

}


