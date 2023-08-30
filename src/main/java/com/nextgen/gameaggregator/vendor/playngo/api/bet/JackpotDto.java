package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class JackpotDto {

    // Id of jackpot
    @PositiveOrZero
    @JacksonXmlProperty(localName = "id")
    private Integer id;

    // Jackpot contribution
    @Positive
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "loss")
    private BigDecimal loss;
}

