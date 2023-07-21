package com.nextgen.gameaggregator.vendor.playngo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {

    @JacksonXmlProperty(localName = "externalGameSessionId")
    private String externalGameSessionId;
}
