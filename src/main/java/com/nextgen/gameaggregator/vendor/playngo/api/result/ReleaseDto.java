package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "release")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseDto {

    @NotBlank
    @JacksonXmlProperty(localName = "username")
    private String username;


}
