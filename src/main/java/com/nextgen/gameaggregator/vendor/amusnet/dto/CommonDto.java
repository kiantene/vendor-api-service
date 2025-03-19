package com.nextgen.gameaggregator.vendor.amusnet.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommonDto {
    @JacksonXmlProperty(localName = "UserName")
    @NotBlank
    @Size(max = 255)
    private String userName; //credential that provided by vendor

    @JacksonXmlProperty(localName = "Password")
    @NotBlank
    @Size(max = 255)
    private String password; //credential that provided by vendor

    @JacksonXmlProperty(localName = "PlayerId")
    @NotBlank
    @Size(max = 50)
    private String playerId;

    @JacksonXmlProperty(localName = "TransferId")
    @NotBlank
    @Size(max = 255)
    private String transferId;

    @JacksonXmlProperty(localName = "GameId")
    @NotBlank
    @Size(max = 255)
    private String vendorGameId;

    @JacksonXmlProperty(localName = "GameNumber")
    @NotBlank
    @Size(max = 255)
    private String gameNumber;
    
    @JacksonXmlProperty(localName = "Amount")
    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal amount;

    @JacksonXmlProperty(localName = "Currency")
    @NotBlank
    @Size(max = 5)
    private String currency;

    @JacksonXmlProperty(localName = "Reason")
    @NotBlank
    private String reason;

    @JacksonXmlProperty(localName = "PortalCode")
    @NotBlank
    @Size(max = 255)
    private String portalCode; //credential that provided by vendor

}
