package com.nextgen.gameaggregator.vendor.ifg.api.login;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.ifg.constant.ResponseCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GameDto {

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    @Size(max = 255)
    private String name;
}
