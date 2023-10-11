package com.nextgen.gameaggregator.vendor.ifg.api.endround;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoundNumDto {
    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    private String id;
}
