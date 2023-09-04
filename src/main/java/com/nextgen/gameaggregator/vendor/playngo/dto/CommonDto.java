package com.nextgen.gameaggregator.vendor.playngo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {

    @Size(max = 64)
    @JacksonXmlProperty(localName = "externalGameSessionId")
    private String externalGameSessionId;

    @NotBlank
    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "accessToken")
    private String accessToken;

    @NotBlank
    @Size(max = 16)
    @JacksonXmlProperty(localName = "gameId")
    protected String gameId;

}
