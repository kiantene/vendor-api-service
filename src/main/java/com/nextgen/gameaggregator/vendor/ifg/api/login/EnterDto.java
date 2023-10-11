package com.nextgen.gameaggregator.vendor.ifg.api.login;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.ifg.constant.ResponseCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data

public class EnterDto {
    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String id;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String guid;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    private String key;

    @JacksonXmlProperty(localName = "game")
    @NotNull(message = ResponseCodes.GAME_NOT_ALLOWED)
    private GameDto game;
}
