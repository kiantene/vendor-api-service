package com.nextgen.gameaggregator.vendor.avatarux.api.cancel;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelDto implements RollbackData {

    @NotBlank
    private String authorization;

    @NotBlank
    private String xServerAuthorization;

    @NotBlank
    @Size(max = 255)
    private String nativeId;

    @NotBlank
    @Size(max = 255)
    private String transactionId;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    @Override
    public String getRollbackId() {
        return this.transactionId;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }
}