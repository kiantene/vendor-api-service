package com.nextgen.gameaggregator.vendor.crystal.api.refund;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.crystal.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundDto extends CommonDto implements RollbackData {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("roundId")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transactionId")
    private String transactionId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameCode")
    private String gameCode;


    @Override
    public String getRollbackId() {
        return this.transactionId;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }
}
