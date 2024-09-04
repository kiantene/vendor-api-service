package com.nextgen.gameaggregator.vendor.cg.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundDto implements RollbackData {
    @NotBlank
    @Size(max = 255)
    private String channelId;
    @NotBlank
    @Size(max = 255)
    private String mtcode;
    @NotBlank
    @Size(max = 255)
    private String roundId;
    @NotBlank
    @Size(max = 50)
    private String accountId;


    @Override
    public String getRollbackId() {
        return this.mtcode;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
