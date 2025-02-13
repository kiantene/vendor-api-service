package com.nextgen.gameaggregator.vendor.ag.event.eventrollback;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Record")
public class EventRollBackDto {

    @NotBlank
    @Size(max = 255)
    @JacksonXmlProperty(localName = "sessionToken")
    private String sessionToken;

    @NotBlank
    @Size(max = 50)
    @JacksonXmlProperty(localName = "playname")
    private String playName;

    @NotBlank
    @Size(max = 255)
    @JacksonXmlProperty(localName = "transactionType")
    private String transactionType;

    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = "^[a-zA-Z]{1,5}$")
    @JacksonXmlProperty(localName = "currency")
    private String currency;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JacksonXmlProperty(localName = "amount")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    @JacksonXmlProperty(localName = "transactionID")
    private String transactionID;

    @NotBlank
    @Size(max = 255)
    @JacksonXmlProperty(localName = "eventID")
    private String eventID;

}