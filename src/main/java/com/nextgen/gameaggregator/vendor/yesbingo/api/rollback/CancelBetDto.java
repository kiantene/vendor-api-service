package com.nextgen.gameaggregator.vendor.yesbingo.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto implements RollbackData {

    // Already validated in GeneralAction. Action id to cancel bet
    public Integer action;

    // timestamp
    @NotNull
    @Positive
    public Long ts;

    // player id
    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+$")
    @Size(max = 50)
    public String uid;

    // This is the bet id
    @NotNull
    @Positive
    public Long transferId;

    @Override
    public String getRollbackId() {
        return this.transferId.toString();
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
