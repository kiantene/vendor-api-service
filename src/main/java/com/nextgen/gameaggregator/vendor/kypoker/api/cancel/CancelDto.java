package com.nextgen.gameaggregator.vendor.kypoker.api.cancel;

import com.nextgen.gameaggregator.core.RequestIdempotency;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CancelDto implements RollbackData, RequestIdempotency {

    @NotBlank
    @Size(min = 1, max = 36)
    private String s;

    @NotBlank
    @Size(min = 1, max = 20)
    private String account;

    @NotBlank
    private String orderId;

    @NotBlank
    @Size(min = 1, max = 36)
    private String gameNo;

    @NotBlank
    @Size(min = 1, max = 36)
    private String gameId;

    @NotNull
    @Digits(integer = 5, fraction = 0)
    private Integer kindId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal money;

    @NotBlank
    @Size(min = 1, max = 36)
    private String currency;

    private Long timeStamp;

    @Override
    public String getRollbackId() {
        return this.orderId;
    }

    @Override
    public Long getVendorSettledTime() {
        return this.timeStamp;
    }

    @Override
    public String getRoundId() {
        return this.gameNo;
    }

    @Override
    public String getTransactionId() {
        return getRollbackId();
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.account;
    }
}
