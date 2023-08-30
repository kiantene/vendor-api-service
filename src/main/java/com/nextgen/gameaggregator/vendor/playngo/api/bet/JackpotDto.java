package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class JackpotDto {

    // Id of jackpot
    @PositiveOrZero
    @JacksonXmlProperty(localName = "id")
    private Integer id;

    // Jackpot contribution
    //@Digits(integer = 13, fraction = 4)
    @Positive
    @JacksonXmlProperty(localName = "loss")
    private BigDecimal loss;
}

