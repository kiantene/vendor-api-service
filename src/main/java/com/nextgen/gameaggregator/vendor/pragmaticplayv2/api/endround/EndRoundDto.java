package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDto {
    private String hash;

    @NotBlank
    private String userId;

    @NotBlank
    private String gameId;

    @NotBlank
    private String roundId;

    private String providerId;

    // Token of the player from Authenticate response.
    @NotBlank
    private String token;
}
