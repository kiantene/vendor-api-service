package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonJackpotDto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class JackpotDto extends CommonJackpotDto {

    // Jackpot win
    @Positive
    @Digits(integer = 12, fraction = 8)
    @JacksonXmlProperty(localName = "win")
    private BigDecimal win;

}

