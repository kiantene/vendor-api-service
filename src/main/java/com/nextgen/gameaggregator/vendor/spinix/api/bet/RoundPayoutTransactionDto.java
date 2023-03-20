package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.BooleanString;
import org.springframework.context.annotation.Conditional;

import javax.annotation.meta.When;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoundPayoutTransactionDto {

    @NotBlank
    @Pattern(regexp = "[a-zA-Z]+")
    public String type;

    public BigDecimal amount;

    public String timestamp;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(max =50)
    public String id;

    public String reqId;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    public String info;

    @NotNull
    public Boolean isEnd;

    public Long getTimestamp() {
        Instant instant = Instant.parse(this.timestamp);
        return instant.getEpochSecond();
    }

}


