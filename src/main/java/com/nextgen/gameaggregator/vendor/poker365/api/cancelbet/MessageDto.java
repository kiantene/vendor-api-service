package com.nextgen.gameaggregator.vendor.poker365.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto implements RollbackData {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("userId")
    private String userId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameNumber")
    private String gameNumber;

    @Override
    public String getRollbackId() {
        return this.getGameNumber();
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return this.getGameNumber();
    }
}
