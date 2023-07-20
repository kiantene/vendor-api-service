package com.nextgen.gameaggregator.vendor.playngo.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;

//@Data
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@Entity
@JacksonXmlRootElement(localName = "authenticate")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthDto {

    @NotBlank
    @JacksonXmlProperty(localName = "username")
    private String username;

    @JacksonXmlProperty(localName = "password")
    private String password;

    @JacksonXmlProperty(localName = "extra")
    private String extra;

    @JacksonXmlProperty(localName = "productId")
    private String productId;

    @JacksonXmlProperty(localName = "client")
    private String client;

    @JacksonXmlProperty(localName = "CID")
    private String CID;

    @JacksonXmlProperty(localName = "clientIP")
    private String clientIP;

    @JacksonXmlProperty(localName = "contextId")
    private String contextId;

    @JacksonXmlProperty(localName = "accessToken")
    private String accessToken;

    @JacksonXmlProperty(localName = "language")
    private String language;

    @JacksonXmlProperty(localName = "gameId")
    private String gameId;

    @JacksonXmlProperty(localName = "channel")
    private String channel;


}
