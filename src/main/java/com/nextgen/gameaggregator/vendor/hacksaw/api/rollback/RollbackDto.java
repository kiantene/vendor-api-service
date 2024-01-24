package com.nextgen.gameaggregator.vendor.hacksaw.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.hacksaw.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.hacksaw.api.bet.FreeRoundDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto extends ActionDto implements RollbackData {

    @NotBlank
    private String externalPlayerId;

    @NotNull
    private Long amount;

    @NotBlank
    private String currency;

    @NotNull
    private Long gameSessionId;

    @NotBlank
    private String externalSessionId;

    @NotNull
    private Long transactionId;

    // variable to check it is free spin or not
    private FreeRoundDto freeRoundData;

    @Override
    public String getRollbackId() {
        return String.valueOf(this.getTransactionId());
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

}
