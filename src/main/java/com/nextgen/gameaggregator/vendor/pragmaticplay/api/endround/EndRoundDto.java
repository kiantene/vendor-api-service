package com.nextgen.gameaggregator.vendor.pragmaticplay.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
    @Size(min = 1, max = 32)
    private String gameId;

    // Id of the round.
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String roundId;

    // Game Provider id.
    @NotBlank
    private String providerId;

    // Token of the player from Authenticate response.
    @NotBlank
    private String token;
}
