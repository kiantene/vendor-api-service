package com.nextgen.gameaggregator.vendor.ifg.api.login;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "server")
public class LoginServiceDto {
    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String session;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}")
    private String time;

    @JacksonXmlProperty(localName = "enter")
    @NotNull
    private EnterDto enter;
}
