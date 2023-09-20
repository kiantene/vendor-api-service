package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonJackpotDto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class JackpotDto extends CommonJackpotDto {

    // Jackpot contribution
    @Positive
    @Digits(integer = 12, fraction = 8)
    @JacksonXmlProperty(localName = "loss")
    private BigDecimal loss;

}

