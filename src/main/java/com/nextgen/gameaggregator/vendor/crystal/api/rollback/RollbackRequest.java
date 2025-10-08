package com.nextgen.gameaggregator.vendor.crystal.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("currencyCode")
    private String currencyCode;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("playerId")
    private String playerId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("roundId")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transactionId")
    private String transactionId;

    @Size(max = 255)
    @JsonProperty("transactionOriginalId")
    private String transactionOriginalId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameCode")
    private String gameCode;

}
