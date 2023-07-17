package com.nextgen.gameaggregator.vendor.playngo.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playngo.constant.ResponseCodes;
import jakarta.persistence.Entity;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import jakarta.validation.constraints.*;

//@Data
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@Entity
@XmlRootElement(name = "authenticate")
@XmlAccessorType(XmlAccessType.FIELD)
public class AuthDto {


    @XmlAttribute(name = "username")
    private String username;

    @XmlAttribute(name = "productId")
    private String productId;

    @XmlAttribute(name = "CID")
    private String CID;

    @XmlAttribute(name = "clientIP")
    private String clientIP;

    @XmlAttribute(name = "contextId")
    private String contextId;

    @XmlAttribute(name = "accessToken")
    private String accessToken;

    @XmlAttribute(name = "language")
    private String language;

    @XmlAttribute(name = "gameId")
    private String gameId;

    @XmlAttribute(name = "channel")
    private String channel;


}
