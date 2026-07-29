package com.nextgen.gameaggregator.vendor.dblive.api.betcancel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetCancelParamsDto implements RollbackData {
    @NotBlank
    @Size(max = 19)
    private String gameTypeId;
    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long cancelTime;
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private BigDecimal transferNo;
    @NotBlank
    @Size(max = 50)
    private String loginName;
    @NotBlank
    private String currency;
    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String roundNo;

    @Override
    public String getRollbackId() {
        return String.valueOf(transferNo);
    }

    @Override
    public Long getVendorSettledTime() {
        return this.cancelTime;
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
