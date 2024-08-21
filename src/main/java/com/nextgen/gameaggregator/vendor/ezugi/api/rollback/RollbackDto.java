package com.nextgen.gameaggregator.vendor.ezugi.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ezugi.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto extends CommonDto implements RollbackData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String uid;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String transactionId;
    @NotNull
    private String roundId;
    @NotNull
    @Digits(integer = 4, fraction = 0)
    private Integer gameId;
    @NotNull
    private Integer tableId;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 4)
    private String currency;
    @NotNull
    @PositiveOrZero(message = "Negative amount")
    @Digits(integer = 25, fraction = 2, message = "Invalid amount")
    private Double rollbackAmount;

    @Override
    public String getRollbackId() {
        return this.transactionId;
    }

    @Override
    public Long getVendorSettledTime() {
        return this.getTimestamp();
    }

}
