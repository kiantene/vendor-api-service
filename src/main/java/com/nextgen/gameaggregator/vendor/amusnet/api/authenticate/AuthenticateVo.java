package com.nextgen.gameaggregator.vendor.amusnet.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.amusnet.vo.ResponseVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JacksonXmlRootElement(localName = "AuthResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateVo extends ResponseVo {
    @JacksonXmlProperty(localName = "AuthenticationToken")
    private String authenticationToken;

    @Override
    public String getCasinoTransferId() {
        return null;
    }

}
