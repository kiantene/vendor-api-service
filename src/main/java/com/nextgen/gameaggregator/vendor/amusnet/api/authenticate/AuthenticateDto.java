package com.nextgen.gameaggregator.vendor.amusnet.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "AuthRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {

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

    @JacksonXmlProperty(localName = "DefenceCode")
    @Size(max = 50)
    private String defenceCode;

    @JacksonXmlProperty(localName = "AuthenticationToken")
    @Size(max = 50)
    private String authenticationToken;

    @JacksonXmlProperty(localName = "GameId")
    @NotBlank
    @Size(max = 255)
    private String vendorGameId;

    @JacksonXmlProperty(localName = "PortalCode")
    @NotBlank
    @Size(max = 255)
    private String portalCode; //credential that provided by vendor
}
