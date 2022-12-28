package com.nextgen.gameaggregator.vendor.pragmaticplay.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDto {

    // Hash code of the request
    @NotBlank
    private String hash;

    // Identifier of the user within the Casino Operator’s system.
    @NotBlank
    private String userId;

    // Id of the game.
    @NotBlank
    private String gameId;

    // Id of the round.
    @NotBlank
    private String roundId;

    // Game Provider id.
    @NotBlank
    private String providerId;

    // Token of the player from Authenticate response.
    @NotBlank
    private String token;
}
