package com.nextgen.gameaggregator.vendor.kypoker.api.cancel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.core.RequestIdempotency;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CancelDto implements RollbackData, RequestIdempotency {

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("s")
    private String s;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("account")
    private String account;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("orderId")
    private String orderId;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("gameNo")
    private String gameNo;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("gameId")
    private String gameId;

    @NotNull
    @JsonProperty("kindId")
    @Digits(integer = 5, fraction = 0)
    private Integer kindId;

    @NotNull
    @JsonProperty("money")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal money;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    @JsonProperty("currency")
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
        return getS();
    }
}
