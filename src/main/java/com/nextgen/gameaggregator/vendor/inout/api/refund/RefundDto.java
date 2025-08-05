package com.nextgen.gameaggregator.vendor.inout.api.refund;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundDto implements RollbackData {
    @NotBlank
    @Size(max = 255)
    private String user_id;

    @NotBlank
    @Size(max = 255)
    private String currency;

    @NotBlank
    @Size(max = 255)
    private String operator;

    @NotBlank
    @Size(max = 255)
    private String transactionId;

    @NotBlank
    @Size(max = 255)
    private String debitId;

    @NotBlank
    @Size(max = 255)
    private String gameId;

    @NotBlank
    @Size(max = 255)
    private Boolean isFinished;

    @NotBlank
    @Size(max = 255)
    private String amount;

    @Override
    public String getRollbackId() {
        return debitId;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return gameId;
    }
}
