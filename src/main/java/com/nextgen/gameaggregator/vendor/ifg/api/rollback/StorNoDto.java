package com.nextgen.gameaggregator.vendor.ifg.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorNoDto {

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Pattern(regexp = "^roundbet$")
    private String cmd;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String id;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String wlid;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 255)
    private String gameid;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String guid;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Pattern(regexp = "^[0-9]*$")
    private String cash;

    @JacksonXmlProperty(localName = "roundnum")
    @NotNull
    private RoundNumDto roundnum;
}
