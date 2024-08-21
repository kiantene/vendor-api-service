package com.nextgen.gameaggregator.vendor.evolution.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evolution.dto.DebitCreditCancelDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelDto extends DebitCreditCancelDto implements RollbackData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 250)
    private String sid; // Player session token

    @Override
    public String getRollbackId() {
        return this.getTransaction().getRefId();
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
