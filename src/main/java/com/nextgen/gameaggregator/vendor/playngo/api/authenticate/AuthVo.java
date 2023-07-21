package com.nextgen.gameaggregator.vendor.playngo.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.playngo.vo.CommonVo;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "authenticate")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthVo extends CommonVo {

    @JacksonXmlProperty(localName = "externalId")
    private String externalId;
    @JacksonXmlProperty(localName = "userCurrency")
    private String userCurrency;
    @JacksonXmlProperty(localName = "nickname")
    private String nickname;
    @JacksonXmlProperty(localName = "country")
    private String country;
    @JacksonXmlProperty(localName = "birthdate")
    private String birthdate;
    @JacksonXmlProperty(localName = "registration")
    private String registration;
    @JacksonXmlProperty(localName = "language")
    private String language;
    @JacksonXmlProperty(localName = "affiliateId")
    private String affiliateId;
    @JacksonXmlProperty(localName = "real")
    private String real;
    @JacksonXmlProperty(localName = "gender")
    private String gender;
    @JacksonXmlProperty(localName = "externalGameSessionId")
    private String externalGameSessionId;
    @JacksonXmlProperty(localName = "region")
    private String region;
    @JacksonXmlProperty(localName = "gameMode")
    private String gameMode;
    @JacksonXmlProperty(localName = "bonusBalance")
    private String bonusBalance;
}
