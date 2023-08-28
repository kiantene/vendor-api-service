package com.nextgen.gameaggregator.vendor.playngo.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "authenticate")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthDto extends CommonDto {

    @NotBlank
    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "username")
    private String username;
    @JacksonXmlProperty(localName = "password")
    private String password;
    @JacksonXmlProperty(localName = "extra")
    private String extra;

    @Size(min = 1, max = 32)
    @JacksonXmlProperty(localName = "productId")
    private String productId;

    @JacksonXmlProperty(localName = "client")
    private String client;
    @JacksonXmlProperty(localName = "CID")
    private String CID;

    @Size(min = 1, max = 32)
    @JacksonXmlProperty(localName = "clientIP")
    private String clientIP;

    @Size(min = 1, max = 50)
    @JacksonXmlProperty(localName = "contextId")
    private String contextId;

    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "accessToken")
    private String accessToken;

    @Size(min = 1, max = 5)
    @JacksonXmlProperty(localName = "language")
    private String language;

    @Size(min = 1, max = 16)
    @JacksonXmlProperty(localName = "gameId")
    private String gameId;
    @JacksonXmlProperty(localName = "channel")
    private String channel;
}
