package com.nextgen.gameaggregator.vendor.ifg.api.rollback;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RoundNumDto {
    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String id;
}
