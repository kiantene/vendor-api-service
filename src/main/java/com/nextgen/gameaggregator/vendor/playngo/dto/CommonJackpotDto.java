package com.nextgen.gameaggregator.vendor.playngo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonJackpotDto {

    // Id of jackpot
    @PositiveOrZero
    @JacksonXmlProperty(localName = "id")
    private Integer id;

}
