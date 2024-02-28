package com.nextgen.gameaggregator.vendor.bombay.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto implements RollbackData {
    @NotBlank
    private String transaction_uuid;

    @NotBlank
    private String token;

    @NotBlank
    private String request_uuid;

    @NotBlank
    private String reference_transaction_uuid;

    @NotNull
    private Integer amount;

    @NotBlank
    private String currency;

    @NotBlank
    private String round;

    @NotNull
    private String game_id;

    @Override
    public String getRollbackId() {
        return this.reference_transaction_uuid;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis(); //unix timestamp with millisecond;
    }
}
