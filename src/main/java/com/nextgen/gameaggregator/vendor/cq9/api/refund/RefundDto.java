package com.nextgen.gameaggregator.vendor.cq9.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundDto implements RollbackData {
    @NotBlank
    @Size(min = 1, max = 70)
    private String mtcode;

    @NotBlank
    private String account;

    @Override
    public String getRollbackId() {
        return this.mtcode;
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
