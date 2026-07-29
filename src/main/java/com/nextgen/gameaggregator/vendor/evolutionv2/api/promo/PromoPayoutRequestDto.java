package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.evolution.dto.BasicDto;
import com.nextgen.gameaggregator.vendor.evolution.dto.GameDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Evolution v2 promo-payout integration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromoPayoutRequestDto extends BasicDto {

    @NotBlank
    @Size(min = 1, max = 250)
    private String sid;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    private GameDto game;

    @NotNull
    @Valid
    private PromoTransactionDto promoTransaction;
}
