package com.nextgen.gameaggregator.vendor.casinogate.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nextgen.gameaggregator.vendor.casinogate.util.StrictLongDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRequest {
    @Positive
    @JsonDeserialize(using = StrictLongDeserializer.class)
    private Long amount;

    @Size(max = 255)
    private String freeSpinId;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    @NotBlank
    @Size(max = 255)
    private String token;

    @NotBlank
    @Size(max = 255)
    private String transactionId;

    // Injected by CasinoGateSignatureValidator via enrichRequestFields.
    private String vendorPlayerUsername;

}
