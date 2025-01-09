package com.nextgen.gameaggregator.vendor.ag.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Record")
public class BalanceDto {

    @NotBlank
    @Size(max = 255)
    @JacksonXmlProperty(localName = "sessionToken")
    private String sessionToken;

    @NotBlank
    @Size(max = 50)
    @JacksonXmlProperty(localName = "playname")
    private String playName;

    @NotBlank
    @JacksonXmlProperty(localName = "transactionType")
    private String transactionType;
    
}

