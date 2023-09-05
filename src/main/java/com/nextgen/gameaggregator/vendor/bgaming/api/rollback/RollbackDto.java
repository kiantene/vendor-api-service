package com.nextgen.gameaggregator.vendor.bgaming.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto implements RollbackData {
    @NotBlank
    @JsonProperty("user_id")
    private String userId;
    @NotBlank
    @JsonProperty("currency")
    private String currency;
    @NotBlank
    @JsonProperty("game")
    private String game;
    @JsonProperty("game_id")
    private String vendorRoundId;
    @JsonProperty("finished")
    private Boolean finished;
    @JsonProperty("actions")
    private List<ActionDto> actions;
    private String betId;
    private String roundId;
    private Long timestamp;

    @Override
    public String getRollbackId() {
        return this.vendorRoundId;
    }

    @Override
    public Long getVendorSettledTime() {
        return this.timestamp;
    }
}
