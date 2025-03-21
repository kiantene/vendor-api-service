package com.nextgen.gameaggregator.vendor.playtech.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.playtech.vo.CommonVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateVo extends CommonVo {

    private String username;
    private String permanentExternalToken;
    private String currencyCode;
    private String countryCode;

}