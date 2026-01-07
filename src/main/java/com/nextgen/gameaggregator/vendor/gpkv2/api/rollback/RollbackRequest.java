package com.nextgen.gameaggregator.vendor.gpkv2.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.gpkv2.api.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest extends CommonDto {

    @NotBlank
    @JsonProperty("transaction_id")
    private String transactionId;

    @NotBlank
    @JsonProperty("round_id")
    private String roundId;

    @NotBlank
    @JsonProperty("game_token")
    private String gameToken;

    @JsonProperty("finished")
    private Boolean finished;

}
