package com.nextgen.gameaggregator.vendor.poker365.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelDto implements RollbackData {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("key")
    private String key;

    @Valid
    @JsonProperty("message")
    private MessageDto message;

    @Override
    public String getRollbackId() {
        return getMessage().getGameNumber();
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return getMessage().getGameNumber();
    }
}
