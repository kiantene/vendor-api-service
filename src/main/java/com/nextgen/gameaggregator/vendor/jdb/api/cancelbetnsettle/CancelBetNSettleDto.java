package com.nextgen.gameaggregator.vendor.jdb.api.cancelbetnsettle;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetNSettleDto implements RollbackData {
    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String action;

    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long ts;

    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String transferId;

    @NotBlank
    @Size(min = 1, max = 30)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;

    @Override
    public String getRollbackId() {
        return this.transferId;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
