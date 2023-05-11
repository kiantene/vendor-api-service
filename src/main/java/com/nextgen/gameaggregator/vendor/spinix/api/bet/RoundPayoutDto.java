package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoundPayoutDto implements RollbackData {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String userId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(max =24)
    public String gameId;

    @NotBlank
    @Size(min = 3, max =24)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(max =32)
    public String roundId;

    @NotBlank
    @Pattern(regexp = "[a-zA-Z]+")
    @Size(max =24)
    public String gameType;

    public List<RoundPayoutTransactionDto> transactionList;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(max =32)
    public String reqId;

    @Nullable
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(max =50)
    public String userToken;

    @NotNull
    @Digits(fraction=2, integer=32)
    @DecimalMax(value = "100000000000000000000000000000000.00", inclusive = false)
    @DecimalMin(value = "-100000000000000000000000000000000.00", inclusive = false)
    public BigDecimal validTurnover;

    public static RoundPayoutTransactionDto findTransaction(List<RoundPayoutTransactionDto> list, String key) {
        for (RoundPayoutTransactionDto obj : list) {
            String index = obj.getType();
            if(index.equals(key)) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public String getRollbackId() {
        return this.roundId;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
