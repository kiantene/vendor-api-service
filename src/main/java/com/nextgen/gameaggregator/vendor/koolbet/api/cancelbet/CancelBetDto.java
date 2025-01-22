package com.nextgen.gameaggregator.vendor.koolbet.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.koolbet.api.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto extends CommonDto implements RollbackData {

    @NotNull
    @NotBlank
    private String currency;

    @NotNull
    @Positive
    private int game;

    @NotNull
    @Positive
    private long round;

    @NotNull
    @Positive
    private double betAmount;

    @NotNull
    private double winloseAmount;

    @NotNull
    @NotBlank
    private String userId;

    @Override
    public String getRollbackId() {
        return String.valueOf(this.round);
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return null;
    }

}
