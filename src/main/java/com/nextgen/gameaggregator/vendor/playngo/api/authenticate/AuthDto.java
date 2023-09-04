package com.nextgen.gameaggregator.vendor.playngo.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@JacksonXmlRootElement(localName = "authenticate")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthDto extends CommonDto {

    @NotBlank
    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "username")
    private String username;

    @Nullable
    @JacksonXmlProperty(localName = "password")
    private String password;

    @Nullable
    @JacksonXmlProperty(localName = "extra")
    private String extra;

    @Size(min = 1, max = 32)
    @JacksonXmlProperty(localName = "productId")
    private String productId;

    @Nullable
    @JacksonXmlProperty(localName = "client")
    private String client;

    @Nullable
    @JacksonXmlProperty(localName = "CID")
    private String CID;

    @Size(max = 32)
    @JacksonXmlProperty(localName = "clientIP")
    private String clientIP;

    @Size(max = 50)
    @JacksonXmlProperty(localName = "contextId")
    private String contextId;

    @Size(max = 5)
    @JacksonXmlProperty(localName = "language")
    private String language;

    @NotBlank
    @Pattern(regexp = "^(1|2|5)$")
    @JacksonXmlProperty(localName = "channel")
    private String channel;
}
