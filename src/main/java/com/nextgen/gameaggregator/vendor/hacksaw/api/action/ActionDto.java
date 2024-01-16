package com.nextgen.gameaggregator.vendor.hacksaw.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {

    @NotBlank
    @Size(min = 1, max = 64)
    @JsonProperty("action")
    private String action;

    @NotBlank
    @Size(min = 1, max = 64)
    private String secret;

    @NotNull
    private Integer gameId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @NotNull
    private Long roundId;

    // Getter methods set to avoid crash name in BetResultData
    public String getGameId() {
        return this.gameId.toString();
    }

    public String getRoundId() {

        String roundId = null;

        // only bet or win data contain round id
        if (action.equals("Bet") || action.equals("Win")) {
            roundId = this.roundId.toString();
        }

        return roundId;
    }
}
