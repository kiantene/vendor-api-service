package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto {

    @NotBlank
    private String token;
    @NotBlank
    private String hash;
    @NotBlank
    private String userId;
    @NotBlank
    private String gameId;
    @NotBlank
    private String roundId;
    @NotBlank
    private String amount;
    @NotBlank
    private String reference;
    @NotBlank
    private String providerId;
    @Positive
    private Long timestamp;
    @NotBlank
    private String roundDetails;
}
