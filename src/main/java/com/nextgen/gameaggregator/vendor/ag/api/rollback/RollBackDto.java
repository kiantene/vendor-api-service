package com.nextgen.gameaggregator.vendor.ag.api.rollback;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Record")
public class RollBackDto {

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

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JacksonXmlProperty(localName = "amount")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    @JacksonXmlProperty(localName = "transactionID")
    private String transactionID;

    @NotBlank
    @Size(max = 255)
    @JacksonXmlProperty(localName = "roundId")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JacksonXmlProperty(localName = "gameId")
    private String gameId;

    @NotBlank
    @JacksonXmlProperty(localName = "time")
    private String dateTimeString;

    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = "^[a-zA-Z]{1,5}$")
    @JacksonXmlProperty(localName = "currency")
    private String currency;

}