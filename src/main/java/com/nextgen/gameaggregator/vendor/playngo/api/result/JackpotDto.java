package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class JackpotDto {

    // Id of jackpot
    @PositiveOrZero
    @JacksonXmlProperty(localName = "id")
    private Integer id;

    // Jackpot win
    //@Digits(integer = 13, fraction = 4)
    @Positive
    @JacksonXmlProperty(localName = "win")
    private BigDecimal win;
}

