package com.nextgen.gameaggregator.vendor.playngo.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "balance")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {

    @NotBlank
    @JacksonXmlProperty(localName = "username")
    private String username;


}
