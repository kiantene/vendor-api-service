package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class JackpotDto {

    @PositiveOrZero
    @JacksonXmlProperty(localName = "id")
    private Integer id;

    @Digits(integer = 13, fraction = 4)
    @JacksonXmlProperty(localName = "loss")
    private BigDecimal loss;
}

