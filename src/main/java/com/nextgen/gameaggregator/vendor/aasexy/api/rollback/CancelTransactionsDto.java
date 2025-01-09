package com.nextgen.gameaggregator.vendor.aasexy.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.aasexy.dto.GameInfoDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelTransactionsDto implements RollbackData {
    @NotBlank
    @Size(max = 255)
    private String platformTxId;

    @NotBlank
    @Size(max = 50)
    private String userId;

    private String platform;

    private String gameType;

    @NotBlank
    @Size(max = 255)
    private String gameCode;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("roundId")
    private String roundId;

    private GameInfoDto gameInfo;

    @Override
    public String getRollbackId() {
        return this.getPlatformTxId();
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
