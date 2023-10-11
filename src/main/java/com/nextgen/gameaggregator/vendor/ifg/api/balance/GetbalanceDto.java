package com.nextgen.gameaggregator.vendor.ifg.api.balance;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GetbalanceDto {
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
}
