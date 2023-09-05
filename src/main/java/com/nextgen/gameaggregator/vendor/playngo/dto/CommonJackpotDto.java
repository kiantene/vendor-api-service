package com.nextgen.gameaggregator.vendor.playngo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonJackpotDto {

    // Id of jackpot
    @PositiveOrZero
    @JacksonXmlProperty(localName = "id")
    private Integer id;

    // Jackpot contribution
    @Positive
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "loss")
    private BigDecimal loss;

    // Jackpot win
    @Positive
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "win")
    private BigDecimal win;

}
