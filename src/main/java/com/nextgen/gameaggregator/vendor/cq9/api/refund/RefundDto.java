package com.nextgen.gameaggregator.vendor.cq9.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundDto implements RollbackData {
    @NotBlank
    @Size(min = 1, max = 70)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    private String mtcode;

    @Override
    public String getRollbackId() {
        return this.mtcode;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
