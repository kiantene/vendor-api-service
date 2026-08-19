package com.nextgen.gameaggregator.vendor.hacksaw.api.promopayout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromoPayoutDto {

    @NotBlank
    @Size(max = 64)
    private String action;

    @NotBlank
    @Size(max = 64)
    private String secret;

    @NotBlank
    @Size(max = 64)
    private String externalPlayerId;

    private Long promotionId;

    @NotNull
    private Long promoPayoutId;

    @Size(max = 64)
    private String externalPromoId;

    @NotNull
    @PositiveOrZero
    private Long amount;

    @NotBlank
    @Size(max = 4)
    private String currency;
}
