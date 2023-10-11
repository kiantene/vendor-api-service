package com.nextgen.gameaggregator.vendor.ifg.api.bet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoundBetDto {
    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String guid;

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
    @Size(min = 4, max = 4)
    @Pattern(regexp = "^spin$")
    private String type;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Pattern(regexp = "^[0-9]*$")
    private String bet;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(min = 1, max = 1)
    @Pattern(regexp = "^[0]+$")
    private String finished; // always be 0

    @JacksonXmlProperty(localName = "roundnum")
    @NotNull
    private RoundNumDto roundnum;
}
