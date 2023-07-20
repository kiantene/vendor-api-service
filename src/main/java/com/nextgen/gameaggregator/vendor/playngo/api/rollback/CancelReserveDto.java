package com.nextgen.gameaggregator.vendor.playngo.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "cancelReserve")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelReserveDto {

    @NotBlank
    @JacksonXmlProperty(localName = "username")
    private String username;


}
