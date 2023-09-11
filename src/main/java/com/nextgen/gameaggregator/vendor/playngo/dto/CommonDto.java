package com.nextgen.gameaggregator.vendor.playngo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JacksonXmlProperty(localName = "accessToken")
    private String accessToken;

    @NotBlank
    @Size(max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    @JacksonXmlProperty(localName = "gameId")
    protected String gameId;

    @NotBlank
    @Size(min = 1, max = 32)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    @JacksonXmlProperty(localName = "productId")
    private String productId;

}
